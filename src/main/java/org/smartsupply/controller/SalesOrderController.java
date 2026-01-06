package org.smartsupply.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.dto.request.SalesOrderLineRequestDto;
import org.smartsupply.dto.request.SalesOrderRequestDto;
import org.smartsupply.dto.response.SalesOrderResponseDto;
import org.smartsupply.service.SalesOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SalesOrderController {

    private final SalesOrderService salesOrderService;


    @PostMapping
    public ResponseEntity<SalesOrderResponseDto> create(@Valid @RequestBody SalesOrderRequestDto request) {
        SalesOrderResponseDto created = salesOrderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderResponseDto> getById(@PathVariable Long id) {
        SalesOrderResponseDto dto = salesOrderService.getById(id);
        return ResponseEntity.ok(dto);
    }


    @GetMapping
    public ResponseEntity<Page<SalesOrderResponseDto>> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {

        Page<SalesOrderResponseDto> page = salesOrderService.listAll(status, clientId, startDate, endDate, pageable);
        return ResponseEntity.ok(page);
    }


    @PostMapping("/{id}/lines")
    public ResponseEntity<SalesOrderResponseDto> addLine(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody SalesOrderLineRequestDto lineRequest) {

        SalesOrderResponseDto dto = salesOrderService.addLine(orderId, lineRequest);
        return ResponseEntity.ok(dto);
    }


    @PutMapping("/{id}/status")
    public ResponseEntity<SalesOrderResponseDto> updateStatus(
            @PathVariable("id") Long orderId,
            @RequestParam("status") String status) {

        SalesOrderResponseDto dto = salesOrderService.updateStatus(orderId, status);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/ship")
   // @RequireRole({Role.WAREHOUSE_MANAGER, Role.ADMIN})
    public ResponseEntity<Void> shipOrder(@PathVariable Long id, @RequestParam(required = false) String trackingNumber) {
        salesOrderService.shipOrder(id, trackingNumber);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}