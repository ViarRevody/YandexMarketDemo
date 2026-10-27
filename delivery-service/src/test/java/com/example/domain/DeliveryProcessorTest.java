package com.example.domain;

import com.example.http.delivery.CreateDeliveryRequestDto;
import com.example.http.delivery.CreateDeliveryResponseDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryProcessorTest {

    private final DeliveryProcessor deliveryProcessor = new DeliveryProcessor();

    @Test
    void testCreateDelivery() {
        CreateDeliveryRequestDto request = new CreateDeliveryRequestDto(1L, "123 Main St");
        CreateDeliveryResponseDto response = deliveryProcessor.createDelivery(request);

        assertNotNull(response);
        assertEquals(1L, response.orderId());
        assertNotNull(response.courierName());
        assertTrue(response.etaMinutes() >= 15 && response.etaMinutes() <= 60);
    }
}
