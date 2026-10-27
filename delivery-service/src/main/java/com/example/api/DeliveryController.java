package com.example.api;

import com.example.domain.DeliveryProcessor;
import com.example.http.delivery.CreateDeliveryRequestDto;
import com.example.http.delivery.CreateDeliveryResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryProcessor deliveryProcessor;

    @PostMapping
    public CreateDeliveryResponseDto createDelivery(@RequestBody CreateDeliveryRequestDto request) {
        log.info("Creating delivery: request={}", request);
        return deliveryProcessor.createDelivery(request);
    }
}
