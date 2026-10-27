package com.example.http.delivery;

public record CreateDeliveryRequestDto(
        Long orderId,
        String address
) {
}
