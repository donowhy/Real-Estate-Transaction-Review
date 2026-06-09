package org.micro.myservice.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.micro.myservice.logistics.persistence.JpaEventStore;
import org.micro.myservice.order.domain.OrderStatus;
import org.micro.myservice.order.dto.CreateOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
class OrderServiceIntegrationTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private JpaEventStore eventStore;
    @Autowired
    private EntityManager entityManager;

    @Test
    void createsOrderAndOutboxEventInOneTransaction() {
        var response = orderService.create(new CreateOrderRequest(
                "create-order-1", "SKU-1", 2, new BigDecimal("25000"), "Seoul"
        ));
        entityManager.flush();

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_INVENTORY);
        assertThat(eventStore.findUnpublished(10)).hasSize(1);
    }

    @Test
    void returnsExistingOrderForDuplicateIdempotencyKey() {
        var request = new CreateOrderRequest(
                "create-order-2", "SKU-2", 1, new BigDecimal("10000"), "Busan"
        );

        assertThat(orderService.create(request).id()).isEqualTo(orderService.create(request).id());
    }
}
