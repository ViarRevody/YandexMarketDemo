package com.example.domain;


import com.example.domain.db.PaymentEntityMapper;
import com.example.domain.db.PaymentEntityRepository;
import com.example.http.payment.CreatePaymentRequestDto;
import com.example.http.payment.CreatePaymentResponseDto;
import com.example.http.payment.PaymentMethod;
import com.example.http.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentEntityMapper mapper;

    private final PaymentEntityRepository repository;

    public CreatePaymentResponseDto makePayment(CreatePaymentRequestDto request){

        var found=repository.findByOrderId(request.orderId());
        if(found.isPresent()){
            log.info("Заказ уже оплачен ={}", request.orderId());
            return mapper.toResponseDto(found.get());
        }
        var entity =mapper.toEntity(request);
        var status= request.paymentMethod().equals(PaymentMethod.QR) ? PaymentStatus.PAYMENT_FAILED : PaymentStatus.PAYMENT_SUCCEEDED;

        entity.setPaymentStatus(status);

        var savedEntity = repository.save(entity);
        return mapper.toResponseDto(savedEntity);
    }
}
