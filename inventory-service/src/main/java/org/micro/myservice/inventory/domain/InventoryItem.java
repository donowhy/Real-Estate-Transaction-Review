package org.micro.myservice.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {
    @Id
    private String sku;
    private int availableQuantity;
    @Version
    private long version;

    protected InventoryItem() {
    }

    public InventoryItem(String sku, int availableQuantity) {
        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }

    public void reserve(int quantity) {
        if (quantity < 1 || availableQuantity < quantity) {
            throw new InsufficientInventoryException(sku);
        }
        availableQuantity -= quantity;
    }

    public void release(int quantity) {
        availableQuantity += quantity;
    }

    public String getSku() { return sku; }
    public int getAvailableQuantity() { return availableQuantity; }
}
