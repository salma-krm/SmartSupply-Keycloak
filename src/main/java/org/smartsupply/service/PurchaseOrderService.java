package org.smartsupply.service;

import org.smartsupply.dto.request.POLineRequestDto;
import org.smartsupply.dto.request.PurchaseOrderRequestDto;
import org.smartsupply.dto.response.PurchaseOrderResponseDto;

import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto dto);
    List<PurchaseOrderResponseDto> getAllPurchaseOrders();
    PurchaseOrderResponseDto getPurchaseOrderById(Long id);
    PurchaseOrderResponseDto addLineToPurchaseOrder(Long purchaseOrderId, POLineRequestDto lineDto);
    void approvePurchaseOrder(Long purchaseOrderId);

    void markPurchaseOrderAsReceived(Long purchaseOrderId, Long warehouseId);
    void deletePurchaseOrder(Long id);
}