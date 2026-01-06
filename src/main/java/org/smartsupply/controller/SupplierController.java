package org.smartsupply.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smartsupply.annotation.RequireAuth;
import org.smartsupply.annotation.RequireRole;
import org.smartsupply.dto.request.SupplierRequestDto;
import org.smartsupply.dto.request.SupplierUpdateDto;
import org.smartsupply.dto.response.SupplierResponseDto;
import org.smartsupply.dto.response.SupplierSimpleDto;
import org.smartsupply.model.enums.Role;
import org.smartsupply.service.SupplierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    //@RequireRole({Role.ADMIN})
    public ResponseEntity<SupplierResponseDto> create(@Valid @RequestBody SupplierRequestDto req) {
        SupplierResponseDto dto = supplierService.createSupplier(req);
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping
    @RequireAuth
    public ResponseEntity<List<SupplierResponseDto>> list() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/simple")
    @RequireAuth
    public ResponseEntity<List<SupplierSimpleDto>> listSimple() {
        return ResponseEntity.ok(supplierService.getAllSimple());
    }

    @GetMapping("/{id}")
    @RequireAuth
    public ResponseEntity<SupplierResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PutMapping("/{id}")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<SupplierResponseDto> update(@PathVariable Long id, @Valid @RequestBody SupplierUpdateDto req) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, req));
    }

    @DeleteMapping("/{id}")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @RequireAuth
    public ResponseEntity<List<SupplierResponseDto>> search(@RequestParam String q) {
        return ResponseEntity.ok(supplierService.search(q));
    }
}