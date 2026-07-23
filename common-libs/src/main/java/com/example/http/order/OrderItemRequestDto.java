package com.example.http.order;

public record OrderItemRequestDto(
        Long itemId,
        Integer quantity,
        String name
) {
}
