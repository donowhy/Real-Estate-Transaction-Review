package org.micro.myservice.delivery.messaging;

import lombok.RequiredArgsConstructor;
import org.micro.myservice.delivery.service.DeliveryService;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryEventConsumer {
    private final DeliveryService deliveryService;

    @KafkaListener(topics = "${logistics.kafka.topic:logistics.events}",
            autoStartup = "${spring.kafka.listener.auto-startup:true}",
            containerFactory = "logisticsKafkaListenerContainerFactory")
    public void consume(LogisticsEvent event) {
        deliveryService.handle(event);
    }
}
