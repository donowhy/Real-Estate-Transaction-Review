package org.micro.myservice.logistics.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LogisticsEvent(
        UUID eventId,
        String idempotencyKey,
        UUID aggregateId,
        LogisticsEventType type,
        Instant occurredAt,
        int version,
        String sku,
        int quantity,
        BigDecimal amount,
        String deliveryAddress,
        String reason
) {
    public static LogisticsEvent create(
            String idempotencyKey,
            UUID aggregateId,
            LogisticsEventType type,
            String sku,
            int quantity,
            BigDecimal amount,
            String deliveryAddress,
            String reason
    ) {
        return new LogisticsEvent(
                UUID.randomUUID(),
                idempotencyKey,
                aggregateId,
                type,
                Instant.now(),
                1,
                sku,
                quantity,
                amount,
                deliveryAddress,
                reason
        );
    }
}
