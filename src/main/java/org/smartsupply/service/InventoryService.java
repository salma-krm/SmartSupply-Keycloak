package org.smartsupply.service;

import java.util.List;

public interface InventoryService {

    void ensureInventoryExists(Long productId, Long warehouseId);

    void inbound(Long productId, Long warehouseId, Integer qty, String reference);

    void outbound(Long productId, Long warehouseId, Integer qty, String reference);

    void adjustment(Long productId, Long warehouseId, Integer qty, String reference);
    void smartReserve(Long productId, Long mainWarehouseId, Integer qty, String orderRef);
    // returns reservation id string
   String reserve(Long productId, Long warehouseId, Integer qty, String sourceRef, long ttlSeconds);



    void transfer(Long productId, Long sourceWarehouseId, Long targetWarehouseId, Integer qty, String reference);

    List<Long> findWarehousesWithAvailable(Long productId);

    Integer getAvailable(Long productId, Long warehouseId);
}