package com.example.domain.db;


import com.example.http.order.OrderDto;
import com.example.http.order.OrderItemDto;
import org.mapstruct.*;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderEntityMapper {

    @Mapping(target = "customerId", source = "customer.id")
    OrderDto toOrderDto(OrderEntity orderEntity);

    @Mapping(target = "itemId", source = "product.id")
    OrderItemDto toOrderItemDto(OrderItemEntity orderItemEntity);

    default String map(com.example.domain.Address.AddressEntity address) {
        if (address == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(address.getCity())
                .append(", ")
                .append(address.getStreet())
                .append(" ")
                .append(address.getHouse());
        if (address.getApartment() != null && !address.getApartment().isBlank()) {
            sb.append(", кв. ").append(address.getApartment());
        }
        return sb.toString();
    }
}