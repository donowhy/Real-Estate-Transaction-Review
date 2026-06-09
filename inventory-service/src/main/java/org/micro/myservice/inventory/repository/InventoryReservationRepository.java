package org.micro.myservice.inventory.repository;

import java.util.UUID;
import org.micro.myservice.inventory.domain.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
}
