package org.micro.myservice.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.micro.myservice.logistics.event.EventPublisher;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.micro.myservice.logistics.event.LogisticsEventType;
import org.micro.myservice.logistics.persistence.JpaEventStore;
import org.micro.myservice.order.domain.Order;
import org.micro.myservice.order.domain.OrderStatus;
import org.micro.myservice.order.repository.OrderRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private JpaEventStore eventStore;

    private OrderService orderService;
    private Order order;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, eventPublisher, eventStore);
        order = new Order(
                UUID.randomUUID(), "order-key", "SKU-1", 2,
                new BigDecimal("25000"), "Seoul"
        );
    }

    @Test
    void advancesOrderAfterInventoryReservation() {
        LogisticsEvent event = event(LogisticsEventType.INVENTORY_RESERVED, null);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.handle(event);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(eventStore).markProcessed(event.eventId());
    }

    @Test
    void cancelsOrderAndPublishesCompensationSignalOnPaymentFailure() {
        order.inventoryReserved();
        LogisticsEvent event = event(LogisticsEventType.PAYMENT_FAILED, "card rejected");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.handle(event);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.argThat(
                published -> published.type() == LogisticsEventType.ORDER_CANCELLED
                        && "card rejected".equals(published.reason())
        ));
    }

    @Test
    void ignoresDuplicateEvent() {
        LogisticsEvent event = event(LogisticsEventType.INVENTORY_RESERVED, null);
        when(eventStore.wasProcessed(event.eventId())).thenReturn(true);

        orderService.handle(event);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_INVENTORY);
        verify(orderRepository, never()).findById(order.getId());
        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private LogisticsEvent event(LogisticsEventType type, String reason) {
        return LogisticsEvent.create(
                order.getIdempotencyKey(), order.getId(), type, order.getSku(),
                order.getQuantity(), order.getAmount(), order.getDeliveryAddress(), reason
        );
    }
}
