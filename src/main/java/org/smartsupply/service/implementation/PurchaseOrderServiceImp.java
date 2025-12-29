package org.smartsupply.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.dto.request.POLineRequestDto;
import org.smartsupply.dto.request.PurchaseOrderRequestDto;
import org.smartsupply.dto.response.PurchaseOrderResponseDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.PurchaseOrderMapper;
import org.smartsupply.model.entity.POLine;
import org.smartsupply.model.entity.Product;
import org.smartsupply.model.entity.PurchaseOrder;
import org.smartsupply.model.entity.Supplier;
import org.smartsupply.model.enums.POStatus;
import org.smartsupply.repository.*;
import org.smartsupply.service.InventoryService;
import org.smartsupply.service.PurchaseOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PurchaseOrderServiceImp implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final POLineRepository poLineRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderMapper mapper;
    private final InventoryService inventoryService;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto dto) {
        log.info("Creating PurchaseOrder for supplierId={}", dto.getSupplierId());
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found id=" + dto.getSupplierId()));

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .supplier(supplier)
                .status(POStatus.CREATED)
                .build();

        if (dto.getLines() != null) {
            for (POLineRequestDto poLineRequestDto : dto.getLines()) {
                Product product = productRepository.findById(poLineRequestDto.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found id=" + poLineRequestDto.getProductId()));
                POLine line = POLine.builder()
                        .purchaseOrder(purchaseOrder)
                        .product(product)
                        .qty(poLineRequestDto.getQty())
                        .price(poLineRequestDto.getPrice())
                        .build();
                purchaseOrder.getLines().add(line);
            }
        }

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        return mapper.toResponseDto(saved);
    }

    @Override
    public List<PurchaseOrderResponseDto> getAllPurchaseOrders() {
        List<PurchaseOrder> list = purchaseOrderRepository.findAll();
        return list.stream().map(mapper::toResponseDto).toList();
    }

    @Override
    public PurchaseOrderResponseDto getPurchaseOrderById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder not found id=" + id));
        return mapper.toResponseDto(purchaseOrder);
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDto addLineToPurchaseOrder(Long purchaseOrderId, POLineRequestDto lineDto) {
        PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder not found id=" + purchaseOrderId));
        Product p = productRepository.findById(lineDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found id=" + lineDto.getProductId()));

        POLine line = POLine.builder()
                .purchaseOrder(po)
                .product(p)
                .qty(lineDto.getQty())
                .price(lineDto.getPrice())
                .build();
        po.getLines().add(line);
        purchaseOrderRepository.save(po);
        return mapper.toResponseDto(po);
    }

    @Override
    @Transactional
    public void approvePurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder not found id=" + purchaseOrderId));

        if (po.getStatus() == POStatus.APPROVED) {
            log.warn("PurchaseOrder {} already APPROVED", purchaseOrderId);
            return;
        }

        if (po.getStatus() == POStatus.RECEIVED) {
            throw new BusinessException("Cannot approve a PurchaseOrder already marked RECEIVED");
        }

        if (po.getStatus() != POStatus.CREATED) {
            throw new BusinessException("PurchaseOrder must be in CREATED status to be approved");
        }

        po.setStatus(POStatus.APPROVED);
        purchaseOrderRepository.save(po);
        log.info("PurchaseOrder {} status changed to APPROVED", purchaseOrderId);
    }


    @Override
    @Transactional
    public void markPurchaseOrderAsReceived(Long purchaseOrderId, Long warehouseId) {
        PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder not found id=" + purchaseOrderId));

        if (po.getStatus() == POStatus.RECEIVED) {
            log.warn("PurchaseOrder {} already marked as RECEIVED", purchaseOrderId);
            return;
        }

        if (po.getStatus() != POStatus.APPROVED) {
            throw new BusinessException("Only an APPROVED PurchaseOrder can be marked as RECEIVED");
        }


        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found id=" + warehouseId);
        }


        for (POLine line : po.getLines()) {
            Long productId = line.getProduct().getId();
            Integer qty = line.getQty();


            inventoryService.ensureInventoryExists(productId, warehouseId);


            String reference = "PO:" + purchaseOrderId + ":LINE:" + line.getId();
            inventoryService.inbound(productId, warehouseId, qty, reference);

            log.info("Inbound applied for PO {}: product={} qty={} warehouse={} line={}", purchaseOrderId, productId, qty, warehouseId, line.getId());
        }


        po.setStatus(POStatus.RECEIVED);
        purchaseOrderRepository.save(po);
        log.info("PurchaseOrder {} status changed to RECEIVED", purchaseOrderId);
    }

    @Override
    @Transactional
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder not found id=" + id));
        if (po.getStatus() == POStatus.RECEIVED) {
            throw new BusinessException("Impossible de supprimer un PurchaseOrder déjà reçu");
        }
        purchaseOrderRepository.delete(po);
    }


}