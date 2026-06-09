package org.micro.myservice.delivery.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "delivery")
public class Delivery {
    public enum Status { REQUESTED, ASSIGNED, IN_TRANSIT, DELIVERED, CANCELLED }

    @Id
    private UUID orderId;
    private String address;
    @Enumerated(EnumType.STRING)
    private Status status;

    protected Delivery() {
    }

    public Delivery(UUID orderId, String address) {
        this.orderId = orderId;
        this.address = address;
        this.status = Status.REQUESTED;
    }
}
