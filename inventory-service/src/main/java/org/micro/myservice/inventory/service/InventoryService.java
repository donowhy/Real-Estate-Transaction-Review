package org.micro.myservice.inventory.service;

import lombok.RequiredArgsConstructor;
import org.micro.myservice.inventory.domain.InsufficientInventoryException;
import org.micro.myservice.inventory.domain.InventoryItem;
import org.micro.myservice.inventory.domain.InventoryReservation;
import org.micro.myservice.inventory.repository.InventoryItemRepository;
import org.micro.myservice.inventory.repository.InventoryReservationRepository;
import org.micro.myservice.logistics.event.EventPublisher;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.micro.myservice.logistics.event.LogisticsEventType;
import org.micro.myservice.logistics.persistence.JpaEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryItemRepository itemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final EventPublisher eventPublisher;
    private final JpaEventStore eventStore;

    @Transactional
    public void handle(LogisticsEvent event) {
        if (eventStore.wasProcessed(event.eventId())) {
            return;
        }
        if (event.type() == LogisticsEventType.ORDER_CREATED) {
            reserve(event);
        } else if (event.type() == LogisticsEventType.ORDER_CANCELLED) {
            release(event);
        } else {
            return;
        }
        eventStore.markProcessed(event.eventId());
    }

    @Transactional
    public InventoryItem stock(String sku, int quantity) {
        InventoryItem item = itemRepository.findForUpdate(sku).orElseGet(() -> new InventoryItem(sku, 0));
        item.release(quantity);
        return itemRepository.save(item);
    }

    private void reserve(LogisticsEvent event) {
        if (reservationRepository.existsById(event.aggregateId())) {
            return;
        }
        try {
            InventoryItem item = itemRepository.findForUpdate(event.sku())
                    .orElseThrow(() -> new InsufficientInventoryException(event.sku()));
            item.reserve(event.quantity());
            reservationRepository.save(new InventoryReservation(event.aggregateId(), event.sku(), event.quantity()));
            emit(event, LogisticsEventType.INVENTORY_RESERVED, null);
        } catch (InsufficientInventoryException exception) {
            emit(event, LogisticsEventType.INVENTORY_RESERVATION_FAILED, exception.getMessage());
        }
    }

    private void release(LogisticsEvent event) {
        reservationRepository.findById(event.aggregateId()).ifPresent(reservation -> {
            if (reservation.release()) {
                InventoryItem item = itemRepository.findForUpdate(reservation.getSku())
                        .orElseThrow(() -> new IllegalStateException("Reserved inventory item is missing"));
                item.release(reservation.getQuantity());
                emit(event, LogisticsEventType.INVENTORY_RELEASED, null);
            }
        });
    }

    private void emit(LogisticsEvent source, LogisticsEventType type, String reason) {
        eventPublisher.publish(LogisticsEvent.create(
                source.idempotencyKey(), source.aggregateId(), type, source.sku(),
                source.quantity(), source.amount(), source.deliveryAddress(), reason
        ));
    }
}
