package com.example.domain;

import com.example.domain.repository.CourierEntityRepository;
import com.example.domain.repository.DeliveryEntityRepository;
import com.example.kafka.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryEntityRepository deliveryEntityRepository;
    private final CourierEntityRepository courierRepository;

    @Value("${delivery-assigned-topic:delivery-assigned-topic}")
    private String deliveryAssignedTopic;

    public void processOrderPaid(OrderPaidEvent event) {

        var orderId = event.orderId();

        var found = deliveryEntityRepository.findByOrderId(orderId);

        if (found.isPresent()) {
            log.info(
                    "Found order delivery was already assigned: delivery={}",
                    found.get()
            );
            return;
        }

        var assignedDelivery = assignDelivery(orderId);

        sendDeliveryAssignedEvent(assignedDelivery);
    }

    private void sendDeliveryAssignedEvent(
            DeliveryEntity assignedDelivery
    ) {
        log.info(
                "Delivery assigned: deliveryId={}, courier={}, etaMinutes={}",
                assignedDelivery.getId(),
                assignedDelivery.getCourier().getName(),
                assignedDelivery.getEtaMinutes()
        );
    }

    @Transactional
    public DeliveryEntity assignDelivery(Long orderId) {

        CourierEntity courier = courierRepository
                .findFirstByAvailableTrue()
                .orElseThrow(() ->
                        new RuntimeException(
                                "No available courier"
                        )
                );

        DeliveryEntity entity = new DeliveryEntity();

        entity.setOrderId(orderId);
        entity.setCourier(courier);

        entity.setEtaMinutes(
                ThreadLocalRandom.current()
                        .nextInt(10, 45)
        );

        courier.setAvailable(false);

        courierRepository.save(courier);

        return deliveryEntityRepository.save(entity);
    }
}
