package org.micro.myservice.notification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class Notification {
    @Id
    private UUID eventId;
    private UUID orderId;
    private String eventType;
    private Instant sentAt;

    protected Notification() {
    }

    public Notification(UUID eventId, UUID orderId, String eventType, Instant sentAt) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.sentAt = sentAt;
    }
}
