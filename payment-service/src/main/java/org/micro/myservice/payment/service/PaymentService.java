package org.micro.myservice.payment.service;

import lombok.RequiredArgsConstructor;
import org.micro.myservice.logistics.event.EventPublisher;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.micro.myservice.logistics.event.LogisticsEventType;
import org.micro.myservice.logistics.persistence.JpaEventStore;
import org.micro.myservice.payment.domain.Payment;
import org.micro.myservice.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;
    private final JpaEventStore eventStore;

    @Transactional
    public void handle(LogisticsEvent event) {
        if (eventStore.wasProcessed(event.eventId())) {
            return;
        }
        if (event.type() == LogisticsEventType.INVENTORY_RESERVED) {
            approve(event);
        } else if (event.type() == LogisticsEventType.ORDER_CANCELLED) {
            cancel(event);
        } else {
            return;
        }
        eventStore.markProcessed(event.eventId());
    }

    private void approve(LogisticsEvent event) {
        if (paymentRepository.existsById(event.aggregateId())) {
            return;
        }
        try {
            paymentGateway.approve(event.aggregateId(), event.amount());
            paymentRepository.save(new Payment(event.aggregateId(), event.amount()));
            emit(event, LogisticsEventType.PAYMENT_APPROVED, null);
        } catch (PaymentRejectedException exception) {
            emit(event, LogisticsEventType.PAYMENT_FAILED, exception.getMessage());
        }
    }

    private void cancel(LogisticsEvent event) {
        paymentRepository.findById(event.aggregateId()).ifPresent(payment -> {
            if (payment.cancel()) {
                paymentGateway.cancel(event.aggregateId(), event.amount());
                emit(event, LogisticsEventType.PAYMENT_CANCELLED, null);
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
