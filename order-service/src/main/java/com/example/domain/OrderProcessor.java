package com.example.domain;

import com.example.api.OrderPaymentRequest;
import com.example.domain.db.OrderEntity;
import com.example.domain.db.OrderEntityMapper;
import com.example.domain.db.OrderItemEntity;
import com.example.domain.db.OrderJpaRepository;
import com.example.external.DeliveryHttpClient;
import com.example.external.PaymentHttpClient;
import com.example.http.delivery.CreateDeliveryRequestDto;
import com.example.http.order.CreateOrderRequestDto;
import com.example.http.order.OrderStatus;
import com.example.http.payment.CreatePaymentRequestDto;
import com.example.http.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderProcessor {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderEntityMapper orderEntityMapper;
    private final PaymentHttpClient paymentHttpClient;
    private final DeliveryHttpClient deliveryHttpClient;

    public OrderEntity create(CreateOrderRequestDto request) {
        var entity = orderEntityMapper.toEntity(request);
        calculatePricingForOrder(entity);
        entity.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        return orderJpaRepository.save(entity);
    }

    public OrderEntity getOrderOrThrow(Long id) {
        var orderItemEntityOptional = orderJpaRepository.findById(id);
        return orderItemEntityOptional.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity with id `%s` not found".formatted(id)));
    }

    private void calculatePricingForOrder(OrderEntity entity) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        if (entity.getItems() != null) {
            for (OrderItemEntity item : entity.getItems()) {
                if (item.getPriceAtPurchase() == null) {
                    var randomPrice = ThreadLocalRandom.current().nextDouble(100, 5000);
                    item.setPriceAtPurchase(BigDecimal.valueOf(randomPrice));
                }

                int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                totalPrice = item.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(quantity))
                        .add(totalPrice);
            }
        }
        entity.setTotalAmount(totalPrice);
    }

    public OrderEntity processPayment(
            Long id,
            OrderPaymentRequest request
    ) {
        var entity = getOrderOrThrow(id);
        if (!entity.getOrderStatus().equals(OrderStatus.PENDING_PAYMENT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must be in status PENDING_PAYMENT");
        }
        var response = paymentHttpClient.createPayment(CreatePaymentRequestDto.builder()
                .orderId(id)
                .paymentMethod(request.paymentMethod())
                .amount(entity.getTotalAmount())
                .build());

        if (response.paymentStatus().equals(PaymentStatus.PAYMENT_SUCCEEDED)) {
            entity.setOrderStatus(OrderStatus.PAID);
            try {
                var deliveryResponse = deliveryHttpClient.createDelivery(
                        new CreateDeliveryRequestDto(id, entity.getAddress())
                );
                entity.setCourierName(deliveryResponse.courierName());
                entity.setEtaMinutes(deliveryResponse.etaMinutes());
                entity.setOrderStatus(OrderStatus.PENDING_DELIVERY);
            } catch (Exception e) {
                log.error("Failed to schedule delivery for order id={}", id, e);
            }
        } else {
            entity.setOrderStatus(OrderStatus.PAYMENT_FAILED);
        }

        return orderJpaRepository.save(entity);
    }
}
