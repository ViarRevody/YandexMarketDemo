package com.example.kafka;

import com.example.http.payment.PaymentMethod;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderPaidEvent(
        Long orderId,
        Long paymentId,
        BigDecimal amount,
        PaymentMethod paymentMethod
) {
}
