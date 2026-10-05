package com.example.domain;

import com.example.api.OrderPaymentRequest;
import com.example.domain.Address.AddressEntityRepository;
import com.example.domain.Customer.CustomerEntityRepository;
import com.example.domain.Product.ProductEntityRepository;
import com.example.domain.Restaurant.RestaurantEntityRepository;
import com.example.domain.db.OrderEntity;
import com.example.domain.db.OrderJpaRepository;
import com.example.external.PaymentHttpClient;
import com.example.http.order.OrderStatus;
import com.example.http.payment.CreatePaymentResponseDto;
import com.example.http.payment.PaymentMethod;
import com.example.http.payment.PaymentStatus;
import com.example.kafka.DeliveryAssignedEvent;
import com.example.kafka.OrderPaidEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;
    @Mock
    private PaymentHttpClient paymentHttpClient;
    @Mock
    private KafkaTemplate<Long, OrderPaidEvent> kafkaTemplate;
    @Mock
    private CustomerEntityRepository customerRepository;
    @Mock
    private AddressEntityRepository addressRepository;
    @Mock
    private RestaurantEntityRepository restaurantRepository;
    @Mock
    private ProductEntityRepository productRepository;

    @InjectMocks
    private OrderProcessor orderProcessor;

    @BeforeEach
    void setOrderPaidTopic() {
        ReflectionTestUtils.setField(orderProcessor, "orderPaidTopic", "orders.events");
    }

    @Test
    void successfulPaymentSavesOrderBeforePublishingPaidEvent() {
        var order = order(OrderStatus.PENDING_PAYMENT);
        var paymentResponse = paymentResponse(PaymentStatus.PAYMENT_SUCCEEDED);
        when(orderJpaRepository.findById(23L)).thenReturn(Optional.of(order));
        when(paymentHttpClient.createPayment(any())).thenReturn(paymentResponse);
        when(orderJpaRepository.save(order)).thenReturn(order);
        when(kafkaTemplate.send(eq("orders.events"), eq(23L), any(OrderPaidEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        var result = orderProcessor.processPayment(23L, new OrderPaymentRequest(PaymentMethod.CARD));

        assertEquals(OrderStatus.PAID, result.getOrderStatus());

        InOrder calls = inOrder(orderJpaRepository, kafkaTemplate);
        calls.verify(orderJpaRepository).save(order);
        calls.verify(kafkaTemplate).send(eq("orders.events"), eq(23L), any(OrderPaidEvent.class));
    }

    @Test
    void failedPaymentDoesNotPublishPaidEvent() {
        var order = order(OrderStatus.PENDING_PAYMENT);
        when(orderJpaRepository.findById(23L)).thenReturn(Optional.of(order));
        when(paymentHttpClient.createPayment(any()))
                .thenReturn(paymentResponse(PaymentStatus.PAYMENT_FAILED));
        when(orderJpaRepository.save(order)).thenReturn(order);

        var result = orderProcessor.processPayment(23L, new OrderPaymentRequest(PaymentMethod.QR));

        assertEquals(OrderStatus.PAYMENT_FAILED, result.getOrderStatus());
        verify(kafkaTemplate, never()).send(any(String.class), any(Long.class), any(OrderPaidEvent.class));
    }

    @Test
    void deliveryTransitionsUpdateOrderStatus() {
        var order = order(OrderStatus.PAID);
        when(orderJpaRepository.findById(23L)).thenReturn(Optional.of(order));
        when(orderJpaRepository.save(order)).thenReturn(order);

        orderProcessor.processDeliveryAssigned(
                DeliveryAssignedEvent.builder().orderId(23L).build()
        );
        orderProcessor.markDelivered(23L);
        assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());
        assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());
        verify(orderJpaRepository, times(2)).save(order);
    }

    private OrderEntity order(OrderStatus status) {
        var order = new OrderEntity();
        order.setId(23L);
        order.setTotalAmount(new BigDecimal("12.50"));
        order.setOrderStatus(status);
        return order;
    }

    private CreatePaymentResponseDto paymentResponse(PaymentStatus status) {
        return new CreatePaymentResponseDto(
                35L,
                status,
                23L,
                PaymentMethod.CARD,
                new BigDecimal("12.50")
        );
    }
}
