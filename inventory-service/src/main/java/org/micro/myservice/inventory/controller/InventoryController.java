package org.micro.myservice.inventory.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.micro.myservice.inventory.domain.InventoryItem;
import org.micro.myservice.inventory.service.InventoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping("/stock")
    public StockResponse stock(@Valid @RequestBody StockRequest request) {
        InventoryItem item = inventoryService.stock(request.sku(), request.quantity());
        return new StockResponse(item.getSku(), item.getAvailableQuantity());
    }

    public record StockRequest(@NotBlank String sku, @Min(1) int quantity) {
    }

    public record StockResponse(String sku, int availableQuantity) {
    }
}
