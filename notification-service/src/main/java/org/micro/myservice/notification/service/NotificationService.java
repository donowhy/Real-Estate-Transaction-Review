package org.micro.myservice.notification.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.micro.myservice.logistics.event.LogisticsEventType;
import org.micro.myservice.logistics.persistence.JpaEventStore;
import org.micro.myservice.notification.domain.Notification;
import org.micro.myservice.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final JpaEventStore eventStore;

    @Transactional
    public void handle(LogisticsEvent event) {
        if (!isNotifiable(event.type()) || eventStore.wasProcessed(event.eventId())) {
            return;
        }
        notificationSender.send(event);
        notificationRepository.save(new Notification(
                event.eventId(), event.aggregateId(), event.type().name(), Instant.now()
        ));
        eventStore.markProcessed(event.eventId());
    }

    private boolean isNotifiable(LogisticsEventType type) {
        return type == LogisticsEventType.ORDER_CONFIRMED || type == LogisticsEventType.ORDER_CANCELLED;
    }
}
