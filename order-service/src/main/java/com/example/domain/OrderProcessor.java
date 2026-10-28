package com.example.domain;

import com.example.api.OrderPaymentRequest;
import com.example.domain.Address.AddressEntityRepository;
import com.example.domain.Customer.CustomerEntityRepository;
import com.example.domain.OrderStatusHistory.OrderStatusHistoryEntity;
import com.example.domain.Product.ProductEntity;
import com.example.domain.Product.ProductEntityRepository;
import com.example.domain.Restaurant.RestaurantEntityRepository;
import com.example.domain.db.OrderEntity;
import com.example.domain.db.OrderItemEntity;
import com.example.domain.db.OrderJpaRepository;
import com.example.external.DeliveryHttpClient;
import com.example.external.PaymentHttpClient;
import com.example.http.delivery.CreateDeliveryRequestDto;
import com.example.http.order.CreateOrderRequestDto;
import com.example.http.order.OrderStatus;
import com.example.http.payment.CreatePaymentRequestDto;
import com.example.http.payment.CreatePaymentResponseDto;
import com.example.http.payment.PaymentStatus;
import com.example.kafka.DeliveryAssignedEvent;
import com.example.kafka.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderProcessor {

    private final OrderJpaRepository orderJpaRepository;
    private final PaymentHttpClient paymentHttpClient;
    private final DeliveryHttpClient deliveryHttpClient;

    private final CustomerEntityRepository customerRepository;
    private final AddressEntityRepository addressRepository;
    private final RestaurantEntityRepository restaurantRepository;
    private final ProductEntityRepository productRepository;

    @Value("${order-paid-topic:order-paid-topic}")
    private String orderPaidTopic;


    public OrderEntity create(CreateOrderRequestDto request) {

        var customer = customerRepository.findById(request.customerId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Customer not found"
                        )
                );

        var address = addressRepository
                .findByIdAndCustomer(request.addressId(), customer)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Address not found for this customer"
                        )
                );

        var restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Restaurant not found"
                        )
                );

        OrderEntity order = new OrderEntity();
        order.setCustomer(customer);
        order.setAddress(address);
        order.setRestaurant(restaurant);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        addStatusHistory(
                order,
                OrderStatus.PENDING_PAYMENT
        );

        for (var itemRequest : request.items()) {

            ProductEntity product = productRepository
                    .findById(itemRequest.productId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Product not found: "
                                            + itemRequest.productId()
                            )
                    );
            if (!Boolean.TRUE.equals(product.getAvailable())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Product is unavailable: "
                                + product.getName()
                );
            }

            if (!product.getRestaurant().getId()
                    .equals(restaurant.getId())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Product does not belong to selected restaurant"
                );
            }

            if (itemRequest.quantity() == null
                    || itemRequest.quantity() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Product quantity must be greater than zero"
                );
            }
            OrderItemEntity orderItem = new OrderItemEntity();

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            order.getItems().add(orderItem);
        }

        if (order.getItems().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order must contain at least one product"
            );
        }
        calculatePricingForOrder(order);
        return orderJpaRepository.save(order);
    }

    public OrderEntity getOrderOrThrow(Long id) {

        return orderJpaRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Entity with id `%s` not found"
                                        .formatted(id)
                        )
                );
    }

    private void calculatePricingForOrder(OrderEntity order) {

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemEntity item : order.getItems()) {

            BigDecimal itemTotal = item.getPriceAtPurchase()
                    .multiply(
                            BigDecimal.valueOf(item.getQuantity())
                    );

            totalPrice = totalPrice.add(itemTotal);
        }

        order.setTotalAmount(totalPrice);
    }

    public OrderEntity processPayment(
            Long id,
            OrderPaymentRequest request
    ) {

        var entity = getOrderOrThrow(id);

        if (!entity.getOrderStatus()
                .equals(OrderStatus.PENDING_PAYMENT)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order must be in status PENDING_PAYMENT"
            );
        }

        var response = paymentHttpClient.createPayment(
                CreatePaymentRequestDto.builder()
                        .orderId(id)
                        .paymentMethod(request.paymentMethod())
                        .amount(entity.getTotalAmount())
                        .build()
        );

        var status = response.paymentStatus()
                .equals(PaymentStatus.PAYMENT_SUCCEEDED)
                ? OrderStatus.PAID
                : OrderStatus.PAYMENT_FAILED;

        entity.setOrderStatus(status);

        if (status == OrderStatus.PAID) {
            try {
                String fullAddress = entity.getAddress() != null ? entity.getAddress().getCity() + ", " + entity.getAddress().getStreet() : "Main Address";
                var deliveryResponse = deliveryHttpClient.createDelivery(
                        new CreateDeliveryRequestDto(id, fullAddress)
                );
                entity.setOrderStatus(OrderStatus.DELIVERY_ASSIGNED);
            } catch (Exception e) {
                log.error("Failed to schedule delivery via REST for order id={}", id, e);
            }
        }

        return orderJpaRepository.save(entity);
    }

    @Transactional
    public void processDeliveryAssigned(
            DeliveryAssignedEvent event
    ) {

        var order = getOrderOrThrow(event.orderId());

        if (!order.getOrderStatus()
                .equals(OrderStatus.PAID)) {

            processIncorrectDeliveryState(order);
            return;
        }

        order.setOrderStatus(
                OrderStatus.DELIVERY_ASSIGNED
        );
        orderJpaRepository.save(order);

        log.info(
                "Order delivery assigned processed: orderId={}",
                order.getId()
        );
    }

    private void processIncorrectDeliveryState(
            OrderEntity order
    ) {

        if (order.getOrderStatus()
                .equals(OrderStatus.DELIVERY_ASSIGNED)) {

            log.info(
                    "Order delivery already processed: orderId={}",
                    order.getId()
            );

        } else {

            log.error(
                    "Trying to assign delivery but order has incorrect state: state={}",
                    order.getOrderStatus()
            );
        }
    }

    private void addStatusHistory(
            OrderEntity order,
            OrderStatus status
    ) {
        OrderStatusHistoryEntity history =
                OrderStatusHistoryEntity.builder()
                        .order(order)
                        .status(status)
                        .changedAt(LocalDateTime.now())
                        .build();

        order.getStatusHistory().add(history);
    }

    @Transactional
    public OrderEntity markDelivered(Long id) {

        var order = getOrderOrThrow(id);

        if (!order.getOrderStatus()
                .equals(OrderStatus.DELIVERY_ASSIGNED)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order must be in status DELIVERY_ASSIGNED to be marked as delivered"
            );
        }
        order.setOrderStatus(OrderStatus.DELIVERED);

        var saved = orderJpaRepository.save(order);

        log.info(
                "Order marked as delivered: orderId={}",
                order.getId()
        );

        return saved;
    }
}
