package com.example.domain;

import com.example.domain.db.PaymentEntity;
import com.example.domain.db.PaymentEntityMapper;
import com.example.domain.db.PaymentEntityRepository;
import com.example.http.payment.CreatePaymentRequestDto;
import com.example.http.payment.CreatePaymentResponseDto;
import com.example.http.payment.PaymentMethod;
import com.example.http.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentEntityMapper mapper;

    @Mock
    private PaymentEntityRepository repository;

    @InjectMocks
    private PaymentService paymentService;

    private CreatePaymentRequestDto requestDto;
    private PaymentEntity paymentEntity;
    private CreatePaymentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = CreatePaymentRequestDto.builder()
                .orderId(1L)
                .amount(BigDecimal.valueOf(100))
                .paymentMethod(PaymentMethod.CARD)
                .build();

        paymentEntity = new PaymentEntity();
        paymentEntity.setId(10L);
        paymentEntity.setOrderId(1L);
        paymentEntity.setAmount(BigDecimal.valueOf(100));
        paymentEntity.setPaymentMethod(PaymentMethod.CARD);
        paymentEntity.setPaymentStatus(PaymentStatus.PAYMENT_SUCCEEDED);

        responseDto = new CreatePaymentResponseDto(
                10L,
                PaymentStatus.PAYMENT_SUCCEEDED,
                1L,
                PaymentMethod.CARD,
                BigDecimal.valueOf(100)
        );
    }

    @Test
    void testMakePaymentExistingOrder() {
        when(repository.findByOrderId(1L)).thenReturn(Optional.of(paymentEntity));
        when(mapper.toResponseDto(paymentEntity)).thenReturn(responseDto);

        CreatePaymentResponseDto result = paymentService.makePayment(requestDto);

        assertNotNull(result);
        assertEquals(PaymentStatus.PAYMENT_SUCCEEDED, result.paymentStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void testMakePaymentNewOrderSuccess() {
        when(repository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(mapper.toEntity(requestDto)).thenReturn(paymentEntity);
        when(repository.save(any(PaymentEntity.class))).thenReturn(paymentEntity);
        when(mapper.toResponseDto(paymentEntity)).thenReturn(responseDto);

        CreatePaymentResponseDto result = paymentService.makePayment(requestDto);

        assertNotNull(result);
        verify(repository).save(paymentEntity);
    }
}
