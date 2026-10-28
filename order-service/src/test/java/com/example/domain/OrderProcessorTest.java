package com.example.domain;

import com.example.api.OrderPaymentRequest;
import com.example.domain.Address.AddressEntity;
import com.example.domain.Address.AddressEntityRepository;
import com.example.domain.Customer.CustomerEntity;
import com.example.domain.Customer.CustomerEntityRepository;
import com.example.domain.Product.ProductEntity;
import com.example.domain.Product.ProductEntityRepository;
import com.example.domain.Restaurant.RestaurantEntity;
import com.example.domain.Restaurant.RestaurantEntityRepository;
import com.example.domain.db.OrderEntity;
import com.example.domain.db.OrderJpaRepository;
import com.example.external.DeliveryHttpClient;
import com.example.external.PaymentHttpClient;
import com.example.http.delivery.CreateDeliveryResponseDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    @Mock
    private PaymentHttpClient paymentHttpClient;

    @Mock
    private DeliveryHttpClient deliveryHttpClient;

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

    private OrderEntity orderEntity;

    @BeforeEach
    void setUp() {
        orderEntity = new OrderEntity();
        orderEntity.setId(1L);

        CustomerEntity customer = new CustomerEntity();
        customer.setId(100L);
        orderEntity.setCustomer(customer);

        AddressEntity address = new AddressEntity();
        address.setId(10L);
        address.setCity("Moscow");
        address.setStreet("Tverskaya");
        orderEntity.setAddress(address);

        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);
        orderEntity.setRestaurant(restaurant);
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

        assertEquals(OrderStatus.DELIVERY_ASSIGNED, updated.getOrderStatus());
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
        orderEntity.setOrderStatus(OrderStatus.DELIVERED);

        when(orderJpaRepository.findById(1L)).thenReturn(Optional.of(orderEntity));

        assertThrows(ResponseStatusException.class, () ->
                orderProcessor.processPayment(1L, new OrderPaymentRequest(PaymentMethod.CARD))
        );
    }
}
