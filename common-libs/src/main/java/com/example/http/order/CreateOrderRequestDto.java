package com.example.http.order;

import java.util.Set;

public record CreateOrderRequestDto(
        Long customerId,
        Long addressId,
        Long restaurantId,
        Set<OrderItemRequestDto> items
) {
}
