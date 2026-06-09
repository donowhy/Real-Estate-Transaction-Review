package org.micro.myservice.delivery.repository;

import java.util.UUID;
import org.micro.myservice.delivery.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
}
