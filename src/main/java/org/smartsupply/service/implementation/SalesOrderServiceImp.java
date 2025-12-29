package org.smartsupply.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.dto.request.SalesOrderLineRequestDto;
import org.smartsupply.dto.request.SalesOrderRequestDto;
import org.smartsupply.dto.response.SalesOrderResponseDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.exception.StockUnavailableException;
import org.smartsupply.mapper.SalesOrderMapper;
import org.smartsupply.model.entity.*;
import org.smartsupply.model.enums.MovementType;
import org.smartsupply.model.enums.OrderStatus;
import org.smartsupply.repository.*;
import org.smartsupply.service.InventoryService;
import org.smartsupply.service.SalesOrderService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SalesOrderServiceImp implements SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderMapper salesOrderMapper;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Override
    public SalesOrderResponseDto create(SalesOrderRequestDto request) {

        User client = userRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + request.getClientId()));
        if (!client.getIsActive()) {
            throw new BusinessException("Client inactif: " + request.getClientId());
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse non trouvé: " + request.getWarehouseId()));
        if (!warehouse.getActive()) {
            throw new BusinessException("Warehouse inactif: " + request.getWarehouseId());
        }

        SalesOrder order = SalesOrder.builder()
                .client(client)
                .warehouse(warehouse)
                .status(OrderStatus.CREATED)
                .build();


        List<SalesOrderLine> lines = new ArrayList<>();
        if (request.getLines() != null) {
            for (SalesOrderLineRequestDto lineRequestDto : request.getLines()) {
                Product product = productRepository.findById(lineRequestDto.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + lineRequestDto.getProductId()));
                if (!(product.getActive())) {
                    throw new BusinessException("Produit inactif: " + lineRequestDto.getProductId());
                }

                BigDecimal unitPrice = product.getOriginalPrice().add(product.getProfite());
                BigDecimal finalPrice = unitPrice.multiply(BigDecimal.valueOf(lineRequestDto.getQtyOrdered()));
                SalesOrderLine line = SalesOrderLine.builder()
                        .product(product)
                        .qtyOrdered(lineRequestDto.getQtyOrdered())
                        .qtyReserved(0)
                        .price(finalPrice)
                        .salesOrder(order)
                        .build();
                lines.add(line);
            }
        }
        order.setLines(lines);

        SalesOrder saved = salesOrderRepository.save(order);
        log.info("SalesOrder créée id={} clientId={}", saved.getId(), client.getId());
        return salesOrderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderResponseDto getById(Long id) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder non trouvée: " + id));
        return salesOrderMapper.toResponse(order);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<SalesOrderResponseDto> listAll(String statusStr, Long clientId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        OrderStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = OrderStatus.valueOf(statusStr);
            } catch (Exception ex) {
                throw new BusinessException("Status invalide: " + statusStr);
            }
        }

        Page<SalesOrder> page;

        boolean hasStatus = status != null;
        boolean hasClient = clientId != null;
        boolean hasStartEnd = (startDate != null && endDate != null);

        if (hasStatus && hasClient && hasStartEnd) {
            page = salesOrderRepository.findByStatusAndClientIdAndCreatedAtBetween(status, clientId, startDate, endDate, pageable);
        } else if (hasStatus && hasClient) {
            page = salesOrderRepository.findByStatusAndClientId(status, clientId, pageable);
        } else if (hasStatus && hasStartEnd) {
            page = salesOrderRepository.findByStatusAndCreatedAtBetween(status, startDate, endDate, pageable);
        } else if (hasClient && hasStartEnd) {
            page = salesOrderRepository.findByClientIdAndCreatedAtBetween(clientId, startDate, endDate, pageable);
        } else if (hasStatus) {
            page = salesOrderRepository.findByStatus(status, pageable);
        } else if (hasClient) {
            page = salesOrderRepository.findByClientId(clientId, pageable);
        } else if (hasStartEnd) {
            page = salesOrderRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        } else {
            page = salesOrderRepository.findAll(pageable);
        }

        return page.map(salesOrderMapper::toResponse);
    }

    @Override
    public SalesOrderResponseDto addLine(Long orderId, SalesOrderLineRequestDto lineRequest) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder non trouvée: " + orderId));

        Product product = productRepository.findById(lineRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product non trouvé: " + lineRequest.getProductId()));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessException("Produit inactif: " + lineRequest.getProductId());
        }
        BigDecimal unitPrice = product.getOriginalPrice().add(product.getProfite());
        BigDecimal finalPrice = unitPrice.multiply(BigDecimal.valueOf(lineRequest.getQtyOrdered()));
        SalesOrderLine line = SalesOrderLine.builder()
                .product(product)
                .qtyOrdered(lineRequest.getQtyOrdered())
                .qtyReserved(0)
                .price(finalPrice)
                .salesOrder(order)
                .build();
        order.getLines().add(line);
        SalesOrder saved = salesOrderRepository.save(order);
        log.info("Ligne ajoutée orderId={} productId={}", orderId, product.getId());
        return salesOrderMapper.toResponse(saved);
    }

    @Override
     public SalesOrderResponseDto updateStatus(Long orderId, String newStatus) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder non trouvée: " + orderId));

        OrderStatus target;
        try {
            target = OrderStatus.valueOf(newStatus);
        } catch (Exception ex) {
            throw new BusinessException("Status invalide: " + newStatus);
        }

        List<String> warnings =new ArrayList<>();
        boolean allLinesReserved = true;

        if (target == OrderStatus.RESERVED && order.getStatus() == OrderStatus.CREATED) {
            log.info("Tentative de réservation pour la commande {} ...", orderId);
            for (SalesOrderLine line : order.getLines()) {
                Long warehouseId = order.getWarehouse().getId();
                Long productId = line.getProduct().getId();
                String productName = line.getProduct().getName();
                int qtyOrdered = line.getQtyOrdered();
                try{
                    inventoryService.smartReserve(productId,warehouseId,qtyOrdered,"SO"+orderId);
                    line.setQtyReserved(qtyOrdered);
                    log.info(" Produit '{}' réservé avec succès (qty={})", productName, qtyOrdered);
                }catch(StockUnavailableException e){
                   allLinesReserved = false ;
                    String msg = String.format(" Stock insuffisant pour le produit '%s' (id=%d). Commande fournisseur prévue."  ,productName, productId);
                    warnings.add(msg);
                    log.warn(msg);
                }

            }

        }

        if((order.getStatus()== OrderStatus.RESERVED )&& (target==OrderStatus.CANCELED||target==OrderStatus.CREATED)){
            log.info("Libération des quantités réservées pour la commande {} ...", orderId);

            for(SalesOrderLine line : order.getLines()){

                int qtyToRelease = line.getQtyReserved();
                if(qtyToRelease<= 0) continue;

                Long warehouseId = order.getWarehouse().getId();
                Long productId = line.getProduct().getId();
                try{
                    Inventory inventory = inventoryRepository.findWithLockByProductIdAndWarehouseId(productId, warehouseId)
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for productId=" + productId + " warehouseId=" + warehouseId));
                    inventory.setQtyReserved(inventory.getQtyReserved()-qtyToRelease);
                    if(inventory.getQtyReserved()<0) inventory.setQtyReserved(0);
                    inventoryRepository.save(inventory);
                    log.info("Libéré {} unités pour le produit '{}' dans warehouse {}", qtyToRelease, line.getProduct().getName(), warehouseId);
                    line.setQtyReserved(0);
                }catch (Exception e){
                    String msg = String.format("Impossible de libérer le stock pour le produit '%s' (id=%d)",
                            line.getProduct().getName(), productId);
                    warnings.add(msg);
                    log.warn(msg, e);
                }


            }
        }

        if(target == OrderStatus.RESERVED && !allLinesReserved){
            log.info("Au moins une ligne n'a pas pu être réservée -> garder le statut CREATED pour la commande {}", orderId);
        }else {
            order.setStatus(target);
        }
        SalesOrder saved = salesOrderRepository.save(order);

        SalesOrderResponseDto dto = salesOrderMapper.toResponse(saved);
        dto.setWarnings(warnings);
        log.info("SalesOrder id={} nouveau status={}", orderId, target);
        return dto;
    }

//    @Transactional
//    public SalesOrderResponseDto updateStatus(Long orderId, String newStatus) {
//        SalesOrder order = salesOrderRepository.findById(orderId)
//                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder non trouvée: " + orderId));
//
//        OrderStatus target;
//        try {
//            target = OrderStatus.valueOf(newStatus);
//        } catch (Exception ex) {
//            throw new BusinessException("Status invalide: " + newStatus);
//        }
//
//        List<String> warnings = new ArrayList<>();
//        boolean allLinesReserved = true;
//
//
//        if (target == OrderStatus.RESERVED && order.getStatus() == OrderStatus.CREATED) {
//            log.info("Tentative de réservation pour la commande {} ...", orderId);
//            for (SalesOrderLine line : order.getLines()) {
//                Long warehouseId = order.getWarehouse().getId();
//                Long productId = line.getProduct().getId();
//                int qtyOrdered = line.getQtyOrdered();
//                try {
//                    inventoryService.smartReserve(productId, warehouseId, qtyOrdered, "SO" + orderId);
//                    line.setQtyReserved(qtyOrdered);
//                    log.info("Produit '{}' réservé avec succès (qty={})", line.getProduct().getName(), qtyOrdered);
//                } catch (StockUnavailableException e) {
//                    allLinesReserved = false;
//                    String msg = String.format("Stock insuffisant pour le produit '%s' (id=%d). Commande fournisseur prévue.",
//                            line.getProduct().getName(), productId);
//                    warnings.add(msg);
//                    log.warn(msg);
//                }
//            }
//        }
//
//
//        if ((order.getStatus() == OrderStatus.RESERVED) &&
//                (target == OrderStatus.CANCELED || target == OrderStatus.CREATED)) {
//
//            log.info("Libération des quantités réservées pour la commande {} ...", orderId);
//            for (SalesOrderLine line : order.getLines()) {
//                int qtyToRelease = line.getQtyReserved();
//                if (qtyToRelease <= 0) continue;
//
//                Long warehouseId = order.getWarehouse().getId();
//                Long productId = line.getProduct().getId();
//
//                try {
//                    Inventory inventory = inventoryRepository.findWithLockByProductIdAndWarehouseId(productId, warehouseId)
//                            .orElseThrow(() -> new ResourceNotFoundException(
//                                    "Inventory not found for productId=" + productId + " warehouseId=" + warehouseId));
//                    inventory.setQtyReserved(Math.max(0, inventory.getQtyReserved() - qtyToRelease));
//                    inventoryRepository.save(inventory);
//
//                    line.setQtyReserved(0);
//                    log.info("Libéré {} unités pour le produit '{}' dans warehouse {}", qtyToRelease,
//                            line.getProduct().getName(), warehouseId);
//                } catch (Exception e) {
//                    String msg = String.format("Impossible de libérer le stock pour le produit '%s' (id=%d)",
//                            line.getProduct().getName(), productId);
//                    warnings.add(msg);
//                    log.warn(msg, e);
//                }
//            }
//        }
//
//        if (target == OrderStatus.RESERVED && !allLinesReserved) {
//            log.info("Au moins une ligne n'a pas pu être réservée -> garder le statut CREATED pour la commande {}", orderId);
//        } else {
//            order.setStatus(target);
//        }
//
//        SalesOrder saved = salesOrderRepository.save(order);
//        SalesOrderResponseDto dto = salesOrderMapper.toResponse(saved);
//        dto.setWarnings(warnings);
//
//        log.info("SalesOrder id={} nouveau status={}", orderId, target);
//        return dto;
//    }


    @Override
    public void delete(Long id) {
        if (!salesOrderRepository.existsById(id)) {
            throw new ResourceNotFoundException("SalesOrder non trouvée: " + id);
        }
        salesOrderRepository.deleteById(id);
        log.info("SalesOrder supprimée id={}", id);
    }

    @Transactional
    public void shipOrder(Long orderId, String trackingNumber) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder non trouvée: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new BusinessException("Impossible d'expédier une commande annulée");
        }

        Long warehouseId = order.getWarehouse().getId();

        for (SalesOrderLine line : order.getLines()) {
            Long productId = line.getProduct().getId();
            int qtyToShip = line.getQtyReserved();

            if (qtyToShip <= 0) {
                log.info("Aucune quantité réservée à expédier pour productId={} sur orderId={}", productId, orderId);
                continue;
            }

            Inventory inv = inventoryRepository.findWithLockByProductIdAndWarehouseId(productId, warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory introuvable productId=" + productId + " warehouseId=" + warehouseId));

            if (inv.getQtyOnHand() < qtyToShip) {
                log.warn("Stock onHand insuffisant mais réservé présent: onHand={}, reserved={}",
                        inv.getQtyOnHand(), inv.getQtyReserved());
                qtyToShip = Math.min(qtyToShip, inv.getQtyOnHand());
            }

            // Déduire du stock et de la réservation
            inv.setQtyOnHand(inv.getQtyOnHand() - qtyToShip);
            inv.setQtyReserved(inv.getQtyReserved() - qtyToShip);
            inventoryRepository.save(inv);

            line.setQtyReserved(line.getQtyReserved() - qtyToShip);

            log.info("Expédié {} unités pour order={} product={} warehouse={}", qtyToShip, orderId, productId, warehouseId);

            // --- Enregistrement du mouvement OUTBOUND ---
            inventoryMovementRepository.save(InventoryMovement.builder()
                    .inventory(inv)
                    .type(MovementType.OUTBOUND)
                    .qty(qtyToShip)
                    .occurredAt(LocalDateTime.now())
                    .reference("SO:" + orderId)
                    .build());
        }

        order.setStatus(OrderStatus.SHIPPED);
        salesOrderRepository.save(order);

        log.info("SalesOrder id={} marked as SHIPPED", orderId);
    }

}