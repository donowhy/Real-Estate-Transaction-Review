package org.micro.myservice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String idempotencyKey;
    @Column(nullable = false)
    private String sku;
    private int quantity;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private String deliveryAddress;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    private String cancellationReason;
    @Version
    private long version;

    protected Order() {
    }

    public Order(UUID id, String idempotencyKey, String sku, int quantity,
                 BigDecimal amount, String deliveryAddress) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.sku = sku;
        this.quantity = quantity;
        this.amount = amount;
        this.deliveryAddress = deliveryAddress;
        this.status = OrderStatus.PENDING_INVENTORY;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public BigDecimal getAmount() { return amount; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public OrderStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }

    public void inventoryReserved() {
        requireStatus(OrderStatus.PENDING_INVENTORY);
        status = OrderStatus.PENDING_PAYMENT;
    }

    public void paymentApproved() {
        requireStatus(OrderStatus.PENDING_PAYMENT);
        status = OrderStatus.PENDING_DELIVERY;
    }

    public void confirm() {
        requireStatus(OrderStatus.PENDING_DELIVERY);
        status = OrderStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        if (status != OrderStatus.CONFIRMED && status != OrderStatus.CANCELLED) {
            status = OrderStatus.CANCELLED;
            cancellationReason = reason;
        }
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected order status " + expected + " but was " + status);
        }
    }
}
