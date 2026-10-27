package com.example.http.delivery;

public record CreateDeliveryResponseDto(
        Long orderId,
        String courierName,
        Integer etaMinutes
) {
}
