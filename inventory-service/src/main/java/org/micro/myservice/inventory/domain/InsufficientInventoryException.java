package org.micro.myservice.inventory.domain;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String sku) {
        super("Insufficient inventory for sku " + sku);
    }
}
