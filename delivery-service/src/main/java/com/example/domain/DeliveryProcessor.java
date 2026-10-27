package com.example.domain;

import com.example.http.delivery.CreateDeliveryRequestDto;
import com.example.http.delivery.CreateDeliveryResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DeliveryProcessor {

    private static final List<String> COURIERS = List.of(
            "Иван Петров",
            "Алексей Смирнов",
            "Дмитрий Ковалев",
            "Сергей Морозов"
    );

    public CreateDeliveryResponseDto createDelivery(CreateDeliveryRequestDto request) {
        int courierIndex = ThreadLocalRandom.current().nextInt(COURIERS.size());
        String courier = COURIERS.get(courierIndex);
        int eta = ThreadLocalRandom.current().nextInt(15, 60);

        return new CreateDeliveryResponseDto(request.orderId(), courier, eta);
    }
}
