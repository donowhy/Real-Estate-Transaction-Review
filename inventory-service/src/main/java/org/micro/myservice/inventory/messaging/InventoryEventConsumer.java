package org.micro.myservice.inventory.messaging;

import lombok.RequiredArgsConstructor;
import org.micro.myservice.inventory.service.InventoryService;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {
    private final InventoryService inventoryService;

    @KafkaListener(topics = "${logistics.kafka.topic:logistics.events}",
            autoStartup = "${spring.kafka.listener.auto-startup:true}",
            containerFactory = "logisticsKafkaListenerContainerFactory")
    public void consume(LogisticsEvent event) {
        inventoryService.handle(event);
    }
}
