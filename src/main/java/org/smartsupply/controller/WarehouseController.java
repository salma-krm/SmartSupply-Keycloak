package org.smartsupply.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smartsupply.annotation.RequireAuth;
import org.smartsupply.annotation.RequireRole;
import org.smartsupply.dto.request.WarehouseRequestDto;
import org.smartsupply.dto.response.WarehouseDetailDto;
import org.smartsupply.dto.response.WarehouseSimpleDto;
import org.smartsupply.model.enums.Role;
import org.smartsupply.service.WarehouseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    @RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<WarehouseSimpleDto> create(@Valid @RequestBody WarehouseRequestDto req) {
        WarehouseSimpleDto dto = warehouseService.createWarehouse(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }


    @GetMapping
    @RequireAuth
    public ResponseEntity<List<WarehouseSimpleDto>> list() {
        List<WarehouseSimpleDto> dtos = warehouseService.getAllWarehouses();
        return ResponseEntity.ok(dtos);
    }


    @GetMapping("/{id}")
    @RequireAuth
    public ResponseEntity<WarehouseDetailDto> getById(@PathVariable Long id) {
        WarehouseDetailDto dto = warehouseService.getWarehouseById(id);
        return ResponseEntity.ok(dto);
    }


    @PutMapping("/{id}")
    @RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<WarehouseSimpleDto> update(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequestDto req) {
        WarehouseSimpleDto dto = warehouseService.updateWarehouse(id, req);
        return ResponseEntity.ok(dto);
    }


    @DeleteMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}