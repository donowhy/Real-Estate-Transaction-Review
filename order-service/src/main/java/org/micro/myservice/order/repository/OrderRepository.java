package org.micro.myservice.order.repository;

import java.util.Optional;
import java.util.UUID;
import org.micro.myservice.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
