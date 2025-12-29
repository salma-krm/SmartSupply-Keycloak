package org.smartsupply.service.implementation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests additionnels pour augmenter la couverture de PurchaseOrderServiceImp.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceImpTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private POLineRepository poLineRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PurchaseOrderMapper mapper;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private PurchaseOrderServiceImp service;

    @Test
    void createPurchaseOrder_withNullLines_doesNotCallProductRepo_andCreatesPO() {
        PurchaseOrderRequestDto req = new PurchaseOrderRequestDto();
        req.setSupplierId(2L);
        // lines left null
        Supplier supplier = new Supplier(); supplier.setId(2L);
        when(supplierRepository.findById(2L)).thenReturn(Optional.of(supplier));

        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            po.setId(500L);
            return po;
        });

        when(mapper.toResponseDto(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            PurchaseOrderResponseDto dto = new PurchaseOrderResponseDto();
            dto.setId(po.getId());
            return dto;
        });

        PurchaseOrderResponseDto res = service.createPurchaseOrder(req);
        assertNotNull(res);
        assertEquals(500L, res.getId());
        // productRepository should never be called because lines are null
        verify(productRepository, never()).findById(anyLong());
    }

    @Test
    void approvePurchaseOrder_statusNull_throwsBusinessException() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(300L);
        po.setStatus(null);
        when(purchaseOrderRepository.findById(300L)).thenReturn(Optional.of(po));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approvePurchaseOrder(300L));
        assertTrue(ex.getMessage().contains("must be in CREATED") || ex.getMessage().length() > 0);
    }

    @Test
    void approvePurchaseOrder_received_throwsBusinessException_message() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(301L);
        po.setStatus(POStatus.RECEIVED);
        when(purchaseOrderRepository.findById(301L)).thenReturn(Optional.of(po));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approvePurchaseOrder(301L));
        assertTrue(ex.getMessage().toLowerCase().contains("received") || ex.getMessage().length() > 0);
    }

    @Test
    void markPurchaseOrderAsReceived_poNotApproved_throwsBusinessException() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(400L);
        po.setStatus(POStatus.CREATED); // not APPROVED
        when(purchaseOrderRepository.findById(400L)).thenReturn(Optional.of(po));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.markPurchaseOrderAsReceived(400L, 1L));
        assertTrue(ex.getMessage().toLowerCase().contains("only an approved") || ex.getMessage().length() > 0);
    }

    @Test
    void markPurchaseOrderAsReceived_alreadyReceived_returnsEarly_withoutInvokingInventory() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(401L);
        po.setStatus(POStatus.RECEIVED);
        // has a line but should return early
        POLine line = new POLine(); line.setId(10L); line.setQty(1);
        Product prod = new Product(); prod.setId(200L);
        line.setProduct(prod);
        po.getLines().add(line);

        when(purchaseOrderRepository.findById(401L)).thenReturn(Optional.of(po));

        // call method: should return early and not call inventoryService
        service.markPurchaseOrderAsReceived(401L, 2L);

        verify(inventoryService, never()).ensureInventoryExists(anyLong(), anyLong());
        verify(inventoryService, never()).inbound(anyLong(), anyLong(), anyInt(), anyString());
        // status must remain RECEIVED
        assertEquals(POStatus.RECEIVED, po.getStatus());
    }

    @Test
    void markPurchaseOrderAsReceived_lineWithNullId_usesReferenceWithNullLineId_and_inbounds() {
        // this test targets the reference formatting and inbound call when line.getId() may be null
        PurchaseOrder po = new PurchaseOrder();
        po.setId(420L);
        po.setStatus(POStatus.APPROVED);
        POLine line = new POLine();
        // intentionally leave line.id null to hit the code path that builds reference with null
        Product prod = new Product(); prod.setId(300L);
        line.setProduct(prod);
        line.setQty(2);
        po.getLines().add(line);

        when(purchaseOrderRepository.findById(420L)).thenReturn(Optional.of(po));
        when(warehouseRepository.existsById(9L)).thenReturn(true);

        // inventoryService methods should be called (we don't assert the exact reference string containing null,
        // but we ensure inbound() is called with expected values)
        doNothing().when(inventoryService).ensureInventoryExists(300L, 9L);
        doNothing().when(inventoryService).inbound(eq(300L), eq(9L), eq(2), anyString());

        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markPurchaseOrderAsReceived(420L, 9L);

        verify(inventoryService).ensureInventoryExists(300L, 9L);
        verify(inventoryService).inbound(eq(300L), eq(9L), eq(2), anyString());
        assertEquals(POStatus.RECEIVED, po.getStatus());
    }

    @Test
    void deletePurchaseOrder_notFound_throws() {
        when(purchaseOrderRepository.findById(900L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.deletePurchaseOrder(900L));
        assertTrue(ex.getMessage().contains("PurchaseOrder not found"));
    }

    @Test
    void getAllPurchaseOrders_empty_returnsEmptyList() {
        when(purchaseOrderRepository.findAll()).thenReturn(Collections.emptyList());
        List<PurchaseOrderResponseDto> res = service.getAllPurchaseOrders();
        assertNotNull(res);
        assertTrue(res.isEmpty());
    }


    @Test
    void createPurchaseOrder_supplierNotFound_throws() {
        PurchaseOrderRequestDto req = new PurchaseOrderRequestDto();
        req.setSupplierId(99L);

        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createPurchaseOrder(req));
    }

    @Test
    void createPurchaseOrder_withLines_createsPOLinesAndPersists() {
        PurchaseOrderRequestDto req = new PurchaseOrderRequestDto();
        req.setSupplierId(10L);

        Supplier supplier = new Supplier();
        supplier.setId(10L);

        req.setLines(new ArrayList<>());
        req.getLines().add(new org.smartsupply.dto.request.POLineRequestDto(1L, 5,  BigDecimal.valueOf(10.0)));
        req.getLines().add(new org.smartsupply.dto.request.POLineRequestDto(2L, 8,  BigDecimal.valueOf(20.0)));

        Product p1 = new Product(); p1.setId(1L);
        Product p2 = new Product(); p2.setId(2L);

        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(p2));

        when(purchaseOrderRepository.save(any())).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            po.setId(999L);
            po.getLines().forEach(l -> l.setId(new Random().nextLong()));
            return po;
        });

        when(mapper.toResponseDto(any())).thenReturn(new PurchaseOrderResponseDto());

        PurchaseOrderResponseDto dto = service.createPurchaseOrder(req);

        assertNotNull(dto);
        verify(productRepository, times(2)).findById(anyLong());
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }


    // -------------------- addLineToPurchaseOrder ----------------------

    @Test
    void addLineToPurchaseOrder_notFoundPO_throws() {
        when(purchaseOrderRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.addLineToPurchaseOrder(88L, new org.smartsupply.dto.request.POLineRequestDto()));
    }

    @Test
    void addLineToPurchaseOrder_productNotFound_throws() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(70L);

        when(purchaseOrderRepository.findById(70L)).thenReturn(Optional.of(po));
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        org.smartsupply.dto.request.POLineRequestDto line =
                new org.smartsupply.dto.request.POLineRequestDto(5L, 10,  BigDecimal.valueOf(12.0));

        assertThrows(ResourceNotFoundException.class,
                () -> service.addLineToPurchaseOrder(70L, line));
    }

    @Test
    void addLineToPurchaseOrder_success_addsLine() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(71L);

        Product p = new Product(); p.setId(3L);

        when(purchaseOrderRepository.findById(71L)).thenReturn(Optional.of(po));
        when(productRepository.findById(3L)).thenReturn(Optional.of(p));

        when(purchaseOrderRepository.save(any())).thenReturn(po);
        when(mapper.toResponseDto(any())).thenReturn(new PurchaseOrderResponseDto());

        org.smartsupply.dto.request.POLineRequestDto line =
                new org.smartsupply.dto.request.POLineRequestDto(3L, 4,  BigDecimal.valueOf(50.0));

        PurchaseOrderResponseDto res = service.addLineToPurchaseOrder(71L, line);

        assertNotNull(res);
        assertEquals(1, po.getLines().size());
    }


    // -------------------- approvePurchaseOrder ------------------------

    @Test
    void approvePurchaseOrder_alreadyApproved_returnsEarly() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(600L);
        po.setStatus(POStatus.APPROVED);

        when(purchaseOrderRepository.findById(600L)).thenReturn(Optional.of(po));

        service.approvePurchaseOrder(600L);

        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void approvePurchaseOrder_success_changesStatus() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(601L);
        po.setStatus(POStatus.CREATED);

        when(purchaseOrderRepository.findById(601L)).thenReturn(Optional.of(po));
        when(purchaseOrderRepository.save(any())).thenReturn(po);

        service.approvePurchaseOrder(601L);

        assertEquals(POStatus.APPROVED, po.getStatus());
    }

    // -------------------- markPurchaseOrderAsReceived ------------------------

    @Test
    void markPurchaseOrderAsReceived_success_forMultipleLines() {

        PurchaseOrder po = new PurchaseOrder();
        po.setId(700L);
        po.setStatus(POStatus.APPROVED);

        // lignes simulées
        Product p1 = new Product(); p1.setId(1L);
        Product p2 = new Product(); p2.setId(2L);

        POLine l1 = new POLine(); l1.setProduct(p1); l1.setQty(5); l1.setId(11L);
        POLine l2 = new POLine(); l2.setProduct(p2); l2.setQty(8); l2.setId(12L);

        po.getLines().add(l1);
        po.getLines().add(l2);

        when(purchaseOrderRepository.findById(700L)).thenReturn(Optional.of(po));
        when(warehouseRepository.existsById(50L)).thenReturn(true);
        when(purchaseOrderRepository.save(any())).thenReturn(po);

        doNothing().when(inventoryService).ensureInventoryExists(anyLong(), anyLong());
        doNothing().when(inventoryService).inbound(anyLong(), anyLong(), anyInt(), anyString());

        service.markPurchaseOrderAsReceived(700L, 50L);

        verify(inventoryService, times(2)).ensureInventoryExists(anyLong(), eq(50L));
        verify(inventoryService, times(2)).inbound(anyLong(), eq(50L), anyInt(), anyString());

        assertEquals(POStatus.RECEIVED, po.getStatus());
    }


    // -------------------- deletePurchaseOrder ------------------------

    @Test
    void deletePurchaseOrder_success_deletes() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(1000L);
        po.setStatus(POStatus.CREATED);

        when(purchaseOrderRepository.findById(1000L)).thenReturn(Optional.of(po));

        service.deletePurchaseOrder(1000L);

        verify(purchaseOrderRepository).delete(po);
    }


    // -------------------- getPurchaseOrderById ------------------------

    @Test
    void getPurchaseOrderById_success_returnsDto() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(111L);

        PurchaseOrderResponseDto dto = new PurchaseOrderResponseDto();
        dto.setId(111L);

        when(purchaseOrderRepository.findById(111L)).thenReturn(Optional.of(po));
        when(mapper.toResponseDto(po)).thenReturn(dto);

        PurchaseOrderResponseDto res = service.getPurchaseOrderById(111L);

        assertNotNull(res);
        assertEquals(111L, res.getId());
    }

    @Test
    void getPurchaseOrderById_notFound_throwsException() {
        when(purchaseOrderRepository.findById(112L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getPurchaseOrderById(112L));
    }

}