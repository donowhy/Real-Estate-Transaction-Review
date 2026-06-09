package org.micro.myservice.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {
    @Override
    public void send(LogisticsEvent event) {
        log.info("notification sent orderId={} eventType={}", event.aggregateId(), event.type());
    }
}
