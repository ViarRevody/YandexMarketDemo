package com.example.domain.db;

import com.example.http.order.CreateOrderRequestDto;
import com.example.http.order.OrderDto;
import com.example.http.order.OrderItemDto;
import com.example.http.order.OrderItemRequestDto;
import org.mapstruct.*;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderEntityMapper {

    OrderEntity toEntity(CreateOrderRequestDto requestDto);

    OrderItemEntity toEntity(OrderItemRequestDto requestDto);

    OrderItemDto toOrderItemDto(OrderItemEntity orderItemEntity);

    OrderDto toOrderDto(OrderEntity orderEntity);

    @AfterMapping
    default void linkItems(@MappingTarget OrderEntity orderEntity) {
        if (orderEntity.getItems() != null) {
            orderEntity.getItems().forEach(item -> item.setOrder(orderEntity));
        }
    }
}