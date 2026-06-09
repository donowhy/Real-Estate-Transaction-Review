package org.micro.myservice.order.domain;

public enum OrderStatus {
    PENDING_INVENTORY,
    PENDING_PAYMENT,
    PENDING_DELIVERY,
    CONFIRMED,
    CANCELLED
}
