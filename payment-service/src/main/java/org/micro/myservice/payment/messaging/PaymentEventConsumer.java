package org.micro.myservice.payment.messaging;

import lombok.RequiredArgsConstructor;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.micro.myservice.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private final PaymentService paymentService;

    @KafkaListener(topics = "${logistics.kafka.topic:logistics.events}",
            autoStartup = "${spring.kafka.listener.auto-startup:true}",
            containerFactory = "logisticsKafkaListenerContainerFactory")
    public void consume(LogisticsEvent event) {
        paymentService.handle(event);
    }
}
