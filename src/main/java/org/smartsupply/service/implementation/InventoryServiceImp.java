package org.smartsupply.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.exception.StockUnavailableException;
import org.smartsupply.model.entity.*;
import org.smartsupply.model.enums.MovementType;
import org.smartsupply.model.enums.POStatus;
import org.smartsupply.repository.*;
import org.smartsupply.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImp implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryMovementRepository movementRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;



    @Override
    @Transactional
    public void ensureInventoryExists(Long productId, Long warehouseId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found " + productId);
        }
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found " + warehouseId);
        }
        if (!inventoryRepository.existsByProductIdAndWarehouseId(productId, warehouseId)) {
            Product p = productRepository.getReferenceById(productId);
            Warehouse w = warehouseRepository.getReferenceById(warehouseId);
            Inventory inv = Inventory.builder()
                    .product(p)
                    .warehouse(w)
                    .qtyOnHand(0)
                    .qtyReserved(0)
                    .build();
            inventoryRepository.save(inv);
        }
    }

    @Override
    @Transactional
    public void inbound(Long productId, Long warehouseId, Integer qty, String reference) {
        Inventory inv = inventoryRepository.findWithLockByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId=" + productId + " warehouseId=" + warehouseId));
        inv.setQtyOnHand(inv.getQtyOnHand() + qty);
        inventoryRepository.save(inv);

        movementRepository.save(InventoryMovement.builder()
                .inventory(inv)
                .type(MovementType.INBOUND)
                .qty(qty)
                .occurredAt(LocalDateTime.now())
                .reference(reference)
                .build());
    }

    @Override
    @Transactional
    public void outbound(Long productId, Long warehouseId, Integer qty, String reference) {
        Inventory inv = inventoryRepository.findWithLockByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId=" + productId + " warehouseId=" + warehouseId));

        int available = inv.getQtyOnHand() - inv.getQtyReserved();
        if (available < qty) {
            throw new StockUnavailableException("Stock insuffisant. Disponible: " + available + ", demandé: " + qty);
        }

        inv.setQtyOnHand(inv.getQtyOnHand() - qty);
        inventoryRepository.save(inv);

        movementRepository.save(InventoryMovement.builder()
                .inventory(inv)
                .type(MovementType.OUTBOUND)
                .qty(qty)
                .occurredAt(LocalDateTime.now())
                .reference(reference)
                .build());
    }

    @Override
    @Transactional
    public void adjustment(Long productId, Long warehouseId, Integer qty, String reference) {
        Inventory inv = inventoryRepository.findWithLockByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId=" + productId + " warehouseId=" + warehouseId));

        int newQtyOnHand = inv.getQtyOnHand() + qty;
        if (newQtyOnHand < inv.getQtyReserved()) {
            throw new BusinessException("Ajustement invalide: qtyOnHand < qtyReserved");
        }
        inv.setQtyOnHand(newQtyOnHand);
        inventoryRepository.save(inv);

        movementRepository.save(InventoryMovement.builder()
                .inventory(inv)
                .type(MovementType.ADJUSTMENT)
                .qty(qty)
                .occurredAt(LocalDateTime.now())
                .reference(reference)
                .build());
    }
    @Override
    @Transactional
    public void smartReserve(Long productId , Long mainWarehouseId,Integer qty,String reference){
        int availableMain = getAvailable(productId,mainWarehouseId);

        if(availableMain >= qty){
            reserve(productId,mainWarehouseId,qty,reference,3600);
            log.info("Réservé entièrement dans le warehouse principal {}" , mainWarehouseId);
            return;
        }

        if(availableMain > 0){
            reserve(productId,mainWarehouseId,availableMain,reference,3600);
            qty-=availableMain;
            log.info("Réservé partiellement {} dans warehouse principal {}", availableMain, mainWarehouseId);
        }

        List<Long> otherWarehouses = warehouseRepository.findAllIdsExcept(mainWarehouseId);

        for(Long wId : otherWarehouses){
            int available = getAvailable(productId,wId);
            if (available<=0) continue;

            int toReserve = Math.min(qty,available);

            log.info("Transfert de {} unités de warehouse {} vers {}", toReserve, wId, mainWarehouseId);
            transfer(productId, wId, mainWarehouseId, toReserve, reference);

            reserve(productId,mainWarehouseId,toReserve,reference,3600);
            qty -= toReserve;
            log.info("Réservé {} après transfert depuis warehouse {}", toReserve, wId);

            if (qty <= 0) break;
        }

        if (qty > 0) {
            Supplier supplier = getOrCreateDefaultSupplier();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found id=" + productId));

            PurchaseOrder po = PurchaseOrder.builder()
                    .supplier(supplier)
                    .status(POStatus.CREATED)
                    .build();

            BigDecimal unitPrice = product.getOriginalPrice() != null ? product.getOriginalPrice() : BigDecimal.ZERO;

            POLine poLine = POLine.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .qty(qty)
                    .price(unitPrice)
                    .build();

            po.getLines().add(poLine);

            PurchaseOrder saved = purchaseOrderRepository.save(po);

            log.info("PurchaseOrder créé id={} pour productId={} qty={} supplierId={}", saved.getId(), productId, qty, supplier.getId());


            throw new StockUnavailableException("PO_CREATED:" + saved.getId());
        }
    }

    @Override
    @Transactional
    public String reserve(Long productId, Long warehouseId, Integer qty, String sourceRef, long ttlSeconds) {
        Inventory inv = inventoryRepository.findWithLockByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId=" + productId + " warehouseId=" + warehouseId));

        int available = inv.getQtyOnHand() - inv.getQtyReserved();
        if (available < qty) {
            throw new StockUnavailableException("Stock insuffisant pour réservation. Disponible: " + available);
        }

        inv.setQtyReserved(inv.getQtyReserved() + qty);
        inventoryRepository.save(inv);


        movementRepository.save(InventoryMovement.builder()
                .inventory(inv)
                .type(MovementType.ADJUSTMENT)
                .qty(qty)
                .occurredAt(LocalDateTime.now())
                .reference(sourceRef)
                .build());


        return UUID.randomUUID().toString();
    }

    @Override
    @Transactional
    public void transfer(Long productId, Long sourceWarehouseId, Long targetWarehouseId, Integer qty, String reference) {
        if (sourceWarehouseId.equals(targetWarehouseId)) {
            throw new BusinessException("Source and target warehouses must differ");
        }

        Inventory sourceInv = inventoryRepository.findWithLockByProductIdAndWarehouseId(productId, sourceWarehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Source inventory not found for productId=" + productId + " warehouseId=" + sourceWarehouseId));

        int available = sourceInv.getQtyOnHand() - sourceInv.getQtyReserved();
        if (available < qty) {
            throw new BusinessException("Stock insuffisant dans l'entrepôt source. Disponible: " + available);
        }

        sourceInv.setQtyOnHand(sourceInv.getQtyOnHand() - qty);
        inventoryRepository.save(sourceInv);
        movementRepository.save(InventoryMovement.builder()
                .inventory(sourceInv)
                .type(MovementType.OUTBOUND)
                .qty(qty)
                .occurredAt(LocalDateTime.now())
                .reference(reference)
                .build());

        Inventory targetInv = inventoryRepository.findWithLockByProductIdAndWarehouseId(productId, targetWarehouseId)
                .orElseGet(() -> {
                    Inventory inv = Inventory.builder()
                            .product(sourceInv.getProduct())
                            .warehouse(warehouseRepository.getReferenceById(targetWarehouseId))
                            .qtyOnHand(0)
                            .qtyReserved(0)
                            .build();
                    return inventoryRepository.save(inv);
                });

        targetInv.setQtyOnHand(targetInv.getQtyOnHand() + qty);
        inventoryRepository.save(targetInv);
        movementRepository.save(InventoryMovement.builder()
                .inventory(targetInv)
                .type(MovementType.INBOUND)
                .qty(qty)
                .occurredAt(LocalDateTime.now())
                .reference(reference)
                .build());
    }

    @Override
    public List<Long> findWarehousesWithAvailable(Long productId) {
        return inventoryRepository.findWarehouseIdsWithAvailable(productId);
    }

    @Override
    public Integer getAvailable(Long productId, Long warehouseId) {
        Integer v = inventoryRepository.findAvailableByProductIdAndWarehouseId(productId, warehouseId);
        return v == null ? 0 : v;
    }


    private Supplier getOrCreateDefaultSupplier() {
        return supplierRepository.findAll().stream().findFirst().orElseGet(() -> {
            Supplier s = Supplier.builder()
                    .name("UNKNOWN_SUPPLIER")
                    .email("unknown@example.com")
                    .contact("unknown")
                    .build();
            Supplier saved = supplierRepository.save(s);
            log.info("Supplier par défaut créé id={}", saved.getId());
            return saved;
        });
    }
}