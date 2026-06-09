package org.micro.myservice.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import org.micro.myservice.order.domain.Order;
import org.micro.myservice.order.domain.OrderStatus;

public record OrderResponse(
        UUID id,
        String sku,
        int quantity,
        BigDecimal amount,
        String deliveryAddress,
        OrderStatus status,
        String cancellationReason
) implements Serializable {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(), order.getSku(), order.getQuantity(), order.getAmount(),
                order.getDeliveryAddress(), order.getStatus(), order.getCancellationReason()
        );
    }
}
