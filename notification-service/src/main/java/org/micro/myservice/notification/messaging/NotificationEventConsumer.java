package org.micro.myservice.notification.messaging;

import lombok.RequiredArgsConstructor;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.micro.myservice.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final NotificationService notificationService;

    @KafkaListener(topics = "${logistics.kafka.topic:logistics.events}",
            autoStartup = "${spring.kafka.listener.auto-startup:true}",
            containerFactory = "logisticsKafkaListenerContainerFactory")
    public void consume(LogisticsEvent event) {
        notificationService.handle(event);
    }
}
