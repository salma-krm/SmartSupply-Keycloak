package org.smartsupply.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smartsupply.annotation.RequireAuth;
import org.smartsupply.annotation.RequireRole;
import org.smartsupply.dto.request.POLineRequestDto;
import org.smartsupply.dto.request.PurchaseOrderRequestDto;
import org.smartsupply.dto.response.PurchaseOrderResponseDto;
import org.smartsupply.model.enums.Role;
import org.smartsupply.service.PurchaseOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;


    @PostMapping
   // @RequireRole({Role.WAREHOUSE_MANAGER})
    public ResponseEntity<PurchaseOrderResponseDto> create(@Valid @RequestBody PurchaseOrderRequestDto req) {
        PurchaseOrderResponseDto dto = purchaseOrderService.createPurchaseOrder(req);
        return ResponseEntity.status(201).body(dto);
    }


    @GetMapping
    @RequireAuth
    public ResponseEntity<List<PurchaseOrderResponseDto>> list() {
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());
    }


    @GetMapping("/{id}")
    @RequireAuth
    public ResponseEntity<PurchaseOrderResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }


    @PostMapping("/{id}/lines")
    @RequireRole({Role.WAREHOUSE_MANAGER})
    public ResponseEntity<PurchaseOrderResponseDto> addLine(@PathVariable Long id, @Valid @RequestBody POLineRequestDto req) {
        return ResponseEntity.ok(purchaseOrderService.addLineToPurchaseOrder(id, req));
    }


    @PutMapping("/{id}/approve")
   // @RequireRole({Role.ADMIN})
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        purchaseOrderService.approvePurchaseOrder(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/mark-received")
    //@RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<Void> markReceived(@PathVariable Long id, @RequestParam("warehouseId") Long warehouseId) {
        purchaseOrderService.markPurchaseOrderAsReceived(id, warehouseId);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{id}")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.noContent().build();
    }
}