package org.smartsupply.service;

import org.smartsupply.dto.request.SalesOrderRequestDto;
import org.smartsupply.dto.request.SalesOrderLineRequestDto;
import org.smartsupply.dto.response.SalesOrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface SalesOrderService {
    SalesOrderResponseDto create(SalesOrderRequestDto request);
    SalesOrderResponseDto getById(Long id);
    Page<SalesOrderResponseDto> listAll(String status, Long clientId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    SalesOrderResponseDto addLine(Long orderId, SalesOrderLineRequestDto lineRequest);
    SalesOrderResponseDto updateStatus(Long orderId, String newStatus);
    void delete(Long id);

    void shipOrder(Long orderId, String trackingNumber);
}