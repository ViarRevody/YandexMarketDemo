package com.example.domain.db;


import com.example.http.order.CreateOrderRequestDto;
import com.example.http.order.OrderDto;
import org.mapstruct.*;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderEntityMapper {


    OrderEntity toEntity(CreateOrderRequestDto requestDto);

    @AfterMapping
    default void linkItems(@MappingTarget OrderEntity orderEntity) {
        orderEntity.getItems().forEach(item -> item.setOrder(orderEntity));
    }

    OrderDto toOrderDto(OrderEntity orderEntity);
}