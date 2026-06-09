package org.micro.myservice.payment.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {
    public enum Status { APPROVED, CANCELLED }

    @Id
    private UUID orderId;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Status status;

    protected Payment() {
    }

    public Payment(UUID orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = Status.APPROVED;
    }

    public boolean cancel() {
        if (status == Status.CANCELLED) {
            return false;
        }
        status = Status.CANCELLED;
        return true;
    }
}
