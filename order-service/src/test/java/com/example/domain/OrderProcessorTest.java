package com.example.domain;

import com.example.api.OrderPaymentRequest;
import com.example.domain.db.OrderEntity;
import com.example.domain.db.OrderEntityMapper;
import com.example.domain.db.OrderItemEntity;
import com.example.domain.db.OrderJpaRepository;
import com.example.external.DeliveryHttpClient;
import com.example.external.PaymentHttpClient;
import com.example.http.delivery.CreateDeliveryResponseDto;
import com.example.http.order.CreateOrderRequestDto;
import com.example.http.order.OrderItemRequestDto;
import com.example.http.order.OrderStatus;
import com.example.http.payment.CreatePaymentResponseDto;
import com.example.http.payment.PaymentMethod;
import com.example.http.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    @Mock
    private OrderEntityMapper orderEntityMapper;

    @Mock
    private PaymentHttpClient paymentHttpClient;

    @Mock
    private DeliveryHttpClient deliveryHttpClient;

    @InjectMocks
    private OrderProcessor orderProcessor;

    private OrderEntity orderEntity;

    @BeforeEach
    void setUp() {
        orderEntity = new OrderEntity();
        orderEntity.setId(1L);
        orderEntity.setCustomerId(100L);
        orderEntity.setAddress("123 Main St");

        OrderItemEntity item = new OrderItemEntity();
        item.setId(10L);
        item.setItemId(50L);
        item.setQuantity(2);
        item.setName("Test Product");
        item.setOrder(orderEntity);

        orderEntity.setItems(Set.of(item));
    }

    @Test
    void testCreateOrder() {
        CreateOrderRequestDto request = new CreateOrderRequestDto(100L, "123 Main St", Set.of(new OrderItemRequestDto(50L, 2, "Test Product")));

        when(orderEntityMapper.toEntity(request)).thenReturn(orderEntity);
        when(orderJpaRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderEntity created = orderProcessor.create(request);

        assertNotNull(created);
        assertEquals(OrderStatus.PENDING_PAYMENT, created.getOrderStatus());
        assertNotNull(created.getTotalAmount());
        verify(orderJpaRepository).save(orderEntity);
    }

    @Test
    void testProcessPaymentSuccessAndDeliveryAssigned() {
        orderEntity.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        orderEntity.setTotalAmount(BigDecimal.valueOf(200));

        when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(orderEntity));
        when(paymentHttpClient.createPayment(any())).thenReturn(
                new CreatePaymentResponseDto(
                        99L,
                        PaymentStatus.PAYMENT_SUCCEEDED,
                        1L,
                        PaymentMethod.CARD,
                        BigDecimal.valueOf(200)
                )
        );
        when(deliveryHttpClient.createDelivery(any())).thenReturn(
                new CreateDeliveryResponseDto(1L, "Иван Петров", 25)
        );
        when(orderJpaRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderEntity updated = orderProcessor.processPayment(1L, new OrderPaymentRequest(PaymentMethod.CARD));

        assertEquals(OrderStatus.PENDING_DELIVERY, updated.getOrderStatus());
        assertEquals("Иван Петров", updated.getCourierName());
        assertEquals(25, updated.getEtaMinutes());
    }

    @Test
    void testProcessPaymentFailure() {
        orderEntity.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        orderEntity.setTotalAmount(BigDecimal.valueOf(200));

        when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(orderEntity));
        when(paymentHttpClient.createPayment(any())).thenReturn(
                new CreatePaymentResponseDto(
                        99L,
                        PaymentStatus.PAYMENT_FAILED,
                        1L,
                        PaymentMethod.QR,
                        BigDecimal.valueOf(200)
                )
        );
        when(orderJpaRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderEntity updated = orderProcessor.processPayment(1L, new OrderPaymentRequest(PaymentMethod.QR));

        assertEquals(OrderStatus.PAYMENT_FAILED, updated.getOrderStatus());
        verify(deliveryHttpClient, never()).createDelivery(any());
    }

    @Test
    void testProcessPaymentInvalidStatus() {
        orderEntity.setOrderStatus(OrderStatus.PAID);

        when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(orderEntity));

        assertThrows(ResponseStatusException.class, () ->
                orderProcessor.processPayment(1L, new OrderPaymentRequest(PaymentMethod.CARD))
        );
    }
}
