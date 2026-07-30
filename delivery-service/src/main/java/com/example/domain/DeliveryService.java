package com.example.domain;

import com.example.kafka.DeliveryAssignedEvent;
import com.example.kafka.OrderPaidEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryEntityRepository deliveryEntityRepository;
    private final KafkaTemplate<Long, DeliveryAssignedEvent> kafkaTemplate;

    @Value("${delivery-assigned-topic}")
    private String deliveryAssignedTopic;

    public void processOrderPaid(OrderPaidEvent event) {
        var orderId = event.orderId();
        var found=deliveryEntityRepository.findByOrderId(orderId);

        if(found.isPresent()){
            log.info("Found order delivery was already assigned: delivery={}", found.get());
            return;
        }
        var assignDelivery =  assignDelivery(orderId);
        sendDeliveryAssignedEvent(assignDelivery);
    }

    private void sendDeliveryAssignedEvent(DeliveryEntity assignedDelivery) {
        kafkaTemplate.send(
                deliveryAssignedTopic,
                assignedDelivery.getOrderId(),
                DeliveryAssignedEvent.builder()
                        .courierName(assignedDelivery.getCourierName())
                        .orderId(assignedDelivery.getOrderId())
                        .etaMinutes(assignedDelivery.getEtaMinutes())
                        .build()
        ).thenAccept(result -> {
            log.info("delivery assigned event sent: deliveryId={}", assignedDelivery.getId());
        });
    }

    public DeliveryEntity assignDelivery(Long orderId){
        var entity =new DeliveryEntity();
        entity.setOrderId(orderId);
        entity.setCourierName("courier-"+ ThreadLocalRandom.current().nextInt(100));
        entity.setEtaMinutes(ThreadLocalRandom.current().nextInt(10,45));
        return deliveryEntityRepository.save(entity);
    }
}
