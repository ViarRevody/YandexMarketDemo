package com.example.kafka;

import com.example.domain.OrderProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;

@EnableKafka
@Slf4j
@AllArgsConstructor
@Configuration
public class DeliveryAssignedKafkaConsumer {
    private final OrderProcessor orderProcessor;

    @KafkaListener(topics = "${delivery-assigned-topic}",
    containerFactory = "deliveryAssignedEventEventListenerFactory")
    public void listen(DeliveryAssignedEvent event) {
        log.info("Received delivery assigned event: {}", event);
        orderProcessor.processDeliveryAssigned(event);
    }
}
