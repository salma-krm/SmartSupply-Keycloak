package org.smartsupply.repository;

import org.smartsupply.model.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockByProductIdAndWarehouseId(Long productId, Long warehouseId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.warehouse.id = :warehouseId and i.product.id = :productId")
    Optional<Inventory> findByWarehouseIdAndProductIdForUpdate(@Param("warehouseId") Long warehouseId,
                                                               @Param("productId") Long productId);

    List<Inventory> findByProductId(Long productId);

    List<Inventory>findByProduct_Sku(String sku);

    List<Inventory> findByWarehouseId(Long warehouseId);


    boolean existsByProductIdAndWarehouseId(Long productId, Long warehouseId);


    @Query("SELECT i.warehouse.id FROM Inventory i WHERE i.product.id = :productId AND (i.qtyOnHand - i.qtyReserved) > 0 ORDER BY (i.qtyOnHand - i.qtyReserved) DESC")
    List<Long> findWarehouseIdsWithAvailable(@Param("productId") Long productId);

    @Query("SELECT (i.qtyOnHand - i.qtyReserved) FROM Inventory i WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId")
    Integer findAvailableByProductIdAndWarehouseId(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);
}