package org.micro.myservice.order.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.micro.myservice.logistics.event.EventPublisher;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.micro.myservice.logistics.event.LogisticsEventType;
import org.micro.myservice.logistics.persistence.JpaEventStore;
import org.micro.myservice.order.domain.Order;
import org.micro.myservice.order.dto.CreateOrderRequest;
import org.micro.myservice.order.dto.OrderResponse;
import org.micro.myservice.order.repository.OrderRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final JpaEventStore eventStore;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        return orderRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(OrderResponse::from)
                .orElseGet(() -> createNew(request));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "orders", key = "#orderId")
    public OrderResponse find(UUID orderId) {
        return OrderResponse.from(getOrder(orderId));
    }

    @Transactional
    @CacheEvict(cacheNames = "orders", key = "#event.aggregateId()")
    public void handle(LogisticsEvent event) {
        if (eventStore.wasProcessed(event.eventId())) {
            return;
        }

        Order order = getOrder(event.aggregateId());
        switch (event.type()) {
            case INVENTORY_RESERVED -> order.inventoryReserved();
            case PAYMENT_APPROVED -> order.paymentApproved();
            case DELIVERY_REQUESTED -> {
                order.confirm();
                publishFrom(order, LogisticsEventType.ORDER_CONFIRMED, null);
            }
            case INVENTORY_RESERVATION_FAILED, PAYMENT_FAILED, DELIVERY_REQUEST_FAILED -> {
                order.cancel(event.reason());
                publishFrom(order, LogisticsEventType.ORDER_CANCELLED, event.reason());
            }
            default -> {
                return;
            }
        }
        eventStore.markProcessed(event.eventId());
    }

    private OrderResponse createNew(CreateOrderRequest request) {
        Order order = new Order(
                UUID.randomUUID(), request.idempotencyKey(), request.sku(), request.quantity(),
                request.amount(), request.deliveryAddress()
        );
        orderRepository.save(order);
        publishFrom(order, LogisticsEventType.ORDER_CREATED, null);
        return OrderResponse.from(order);
    }

    private void publishFrom(Order order, LogisticsEventType type, String reason) {
        eventPublisher.publish(LogisticsEvent.create(
                order.getIdempotencyKey(), order.getId(), type, order.getSku(),
                order.getQuantity(), order.getAmount(), order.getDeliveryAddress(), reason
        ));
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
