package org.micro.myservice.delivery.service;

import lombok.RequiredArgsConstructor;
import org.micro.myservice.delivery.domain.Delivery;
import org.micro.myservice.delivery.repository.DeliveryRepository;
import org.micro.myservice.logistics.event.EventPublisher;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.micro.myservice.logistics.event.LogisticsEventType;
import org.micro.myservice.logistics.persistence.JpaEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final EventPublisher eventPublisher;
    private final JpaEventStore eventStore;

    @Transactional
    public void handle(LogisticsEvent event) {
        if (event.type() != LogisticsEventType.PAYMENT_APPROVED || eventStore.wasProcessed(event.eventId())) {
            return;
        }
        if (!deliveryRepository.existsById(event.aggregateId())) {
            if (event.deliveryAddress() == null || event.deliveryAddress().isBlank()) {
                emit(event, LogisticsEventType.DELIVERY_REQUEST_FAILED, "Delivery address is required");
            } else {
                deliveryRepository.save(new Delivery(event.aggregateId(), event.deliveryAddress()));
                emit(event, LogisticsEventType.DELIVERY_REQUESTED, null);
            }
        }
        eventStore.markProcessed(event.eventId());
    }

    private void emit(LogisticsEvent source, LogisticsEventType type, String reason) {
        eventPublisher.publish(LogisticsEvent.create(
                source.idempotencyKey(), source.aggregateId(), type, source.sku(),
                source.quantity(), source.amount(), source.deliveryAddress(), reason
        ));
    }
}
