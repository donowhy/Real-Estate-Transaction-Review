package org.micro.myservice.order.messaging;

import lombok.RequiredArgsConstructor;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.micro.myservice.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {
    private final OrderService orderService;

    @KafkaListener(
            topics = "${logistics.kafka.topic:logistics.events}",
            autoStartup = "${spring.kafka.listener.auto-startup:true}",
            containerFactory = "logisticsKafkaListenerContainerFactory"
    )
    public void consume(LogisticsEvent event) {
        orderService.handle(event);
    }
}
