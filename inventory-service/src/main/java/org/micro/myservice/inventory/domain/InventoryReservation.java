package org.micro.myservice.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservation")
public class InventoryReservation {
    public enum Status { RESERVED, RELEASED }

    @Id
    private UUID orderId;
    private String sku;
    private int quantity;
    @Enumerated(EnumType.STRING)
    private Status status;

    protected InventoryReservation() {
    }

    public InventoryReservation(UUID orderId, String sku, int quantity) {
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = Status.RESERVED;
    }

    public boolean release() {
        if (status == Status.RELEASED) {
            return false;
        }
        status = Status.RELEASED;
        return true;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
}
