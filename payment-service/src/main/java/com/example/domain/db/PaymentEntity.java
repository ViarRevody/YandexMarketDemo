package com.example.domain.db;


import com.example.http.payment.PaymentMethod;
import com.example.http.payment.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name="payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name="id", nullable = false)
    private Long id;

    @Column(name="order_id",nullable = false,unique = true)
    private Long orderId;

    @Column(name="amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name="payment_status",nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name="payment_method",nullable = false)
    private PaymentMethod paymentMethod;

}

