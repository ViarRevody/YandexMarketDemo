package com.example.http.order;

public record OrderItemRequestDto(
        Long productId,
        Integer quantity
) {
}
