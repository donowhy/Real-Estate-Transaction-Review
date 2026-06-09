package org.micro.myservice.inventory.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.micro.myservice.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from InventoryItem item where item.sku = :sku")
    Optional<InventoryItem> findForUpdate(@Param("sku") String sku);
}
