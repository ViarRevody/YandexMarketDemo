package com.example.domain.db;

import com.example.domain.Address.AddressEntity;
import com.example.domain.Customer.CustomerEntity;
import com.example.domain.Product.ProductEntity;
import com.example.http.order.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderEntityMapperTest {

    private final OrderEntityMapper mapper = new OrderEntityMapperImpl();

    @Test
    void mapsCustomerAndProductIdsIntoOrderResponse() {
        var customer = new CustomerEntity();
        customer.setId(7L);

        var address = new AddressEntity();
        address.setCity("Москва");
        address.setStreet("Ленина");
        address.setHouse("1");

        var product = new ProductEntity();
        product.setId(42L);

        var item = new OrderItemEntity();
        item.setProduct(product);
        item.setQuantity(2);
        item.setPriceAtPurchase(new BigDecimal("50.00"));

        var order = new OrderEntity();
        order.setId(23L);
        order.setCustomer(customer);
        order.setAddress(address);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setItems(Set.of(item));

        var response = mapper.toOrderDto(order);
        var responseItem = response.items().iterator().next();

        assertEquals(7L, response.customerId());
        assertEquals("Москва, Ленина 1", response.address());
        assertEquals(42L, responseItem.itemId());
    }
}
