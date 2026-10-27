package com.example.external;

import com.example.http.delivery.CreateDeliveryRequestDto;
import com.example.http.delivery.CreateDeliveryResponseDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(
        accept = "application/json",
        contentType = "application/json",
        url = "/api/deliveries"
)
public interface DeliveryHttpClient {
    @PostExchange
    CreateDeliveryResponseDto createDelivery(
            @RequestBody CreateDeliveryRequestDto request);
}
