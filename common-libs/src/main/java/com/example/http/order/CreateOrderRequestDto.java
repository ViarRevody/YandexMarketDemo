package com.example.http.order;

import java.util.Set;

public record CreateOrderRequestDto(
        Long customerId,
        String address,
        Set<OrderItemDto> items
) {
}
