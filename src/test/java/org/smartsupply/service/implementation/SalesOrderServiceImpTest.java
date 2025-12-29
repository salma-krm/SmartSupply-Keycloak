package org.smartsupply.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smartsupply.dto.request.SalesOrderLineRequestDto;
import org.smartsupply.dto.request.SalesOrderRequestDto;
import org.smartsupply.dto.response.SalesOrderResponseDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.exception.StockUnavailableException;
import org.smartsupply.mapper.SalesOrderMapper;
import org.smartsupply.model.entity.*;
import org.smartsupply.model.enums.OrderStatus;
import org.smartsupply.model.enums.MovementType;
import org.smartsupply.repository.*;
import org.smartsupply.service.InventoryService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceImpTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private SalesOrderMapper salesOrderMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private SalesOrderServiceImp service;

    @BeforeEach
    void setUp() {
        // default save behaviour: return the entity (assign id if null)
        lenient().when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> {
            SalesOrder o = inv.getArgument(0);
            if (o.getId() == null) o.setId(100L);
            return o;
        });
        // default mapper: return a simple response dto so tests that call mapping won't NPE
        lenient().when(salesOrderMapper.toResponse(any(SalesOrder.class))).thenAnswer(inv -> {
            SalesOrder o = inv.getArgument(0);
            SalesOrderResponseDto dto = new SalesOrderResponseDto();
            dto.setId(o.getId());
            dto.setStatus(o.getStatus() == null ? null : o.getStatus().name());
            dto.setWarnings(new ArrayList<>());
            return dto;
        });
    }

    @Test
    void create_success_createsSalesOrder() {
        // prepare request with one line
        SalesOrderRequestDto req = new SalesOrderRequestDto();
        req.setClientId(1L);
        req.setWarehouseId(10L);
        SalesOrderLineRequestDto lineReq = new SalesOrderLineRequestDto();
        lineReq.setProductId(5L);
        lineReq.setQtyOrdered(2);
        req.setLines(Collections.singletonList(lineReq));

        User client = new User();
        client.setId(1L);
        client.setIsActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));

        Warehouse wh = new Warehouse();
        wh.setId(10L);
        wh.setActive(true);
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(wh));

        Product prod = new Product();
        prod.setId(5L);
        prod.setActive(true);
        prod.setOriginalPrice(new BigDecimal("2.0"));
        prod.setProfite(new BigDecimal("1.0"));
        prod.setName("P1");
        when(productRepository.findById(5L)).thenReturn(Optional.of(prod));

        // execute
        SalesOrderResponseDto resp = service.create(req);

        assertNotNull(resp);
        assertEquals(100L, resp.getId());
        verify(salesOrderRepository).save(any(SalesOrder.class));
        verify(salesOrderMapper).toResponse(any(SalesOrder.class));
    }

    @Test
    void create_clientNotFound_throws() {
        SalesOrderRequestDto req = new SalesOrderRequestDto();
        req.setClientId(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.create(req));
        assertTrue(ex.getMessage().contains("Client non trouvé"));
    }

    @Test
    void create_inactiveClient_throws() {
        SalesOrderRequestDto req = new SalesOrderRequestDto();
        req.setClientId(2L);
        User client = new User();
        client.setId(2L);
        client.setIsActive(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertTrue(ex.getMessage().contains("Client inactif"));
    }

    @Test
    void addLine_success_addsLineAndSaves() {
        SalesOrder order = new SalesOrder();
        order.setId(11L);
        order.setLines(new ArrayList<>());
        when(salesOrderRepository.findById(11L)).thenReturn(Optional.of(order));

        Product p = new Product();
        p.setId(7L);
        p.setActive(true);
        p.setOriginalPrice(new BigDecimal("1.0"));
        p.setProfite(new BigDecimal("0.5"));
        when(productRepository.findById(7L)).thenReturn(Optional.of(p));

        SalesOrderLineRequestDto lineReq = new SalesOrderLineRequestDto();
        lineReq.setProductId(7L);
        lineReq.setQtyOrdered(3);

        SalesOrderResponseDto dto = new SalesOrderResponseDto();
        dto.setId(11L);
        when(salesOrderMapper.toResponse(any(SalesOrder.class))).thenReturn(dto);

        SalesOrderResponseDto res = service.addLine(11L, lineReq);

        assertNotNull(res);
        assertEquals(11L, res.getId());
        assertEquals(1, order.getLines().size());
        verify(salesOrderRepository).save(order);
    }

    @Test
    void addLine_productNotFound_throws() {
        SalesOrder order = new SalesOrder();
        order.setId(12L);
        order.setLines(new ArrayList<>());
        when(salesOrderRepository.findById(12L)).thenReturn(Optional.of(order));

        SalesOrderLineRequestDto lineReq = new SalesOrderLineRequestDto();
        lineReq.setProductId(999L);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.addLine(12L, lineReq));
        assertTrue(ex.getMessage().contains("Product non trouvé"));
    }

    @Test
    void updateStatus_reserve_allLinesReserved_setsReserved() {
        // Prepare order in CREATED status with one line
        SalesOrder order = new SalesOrder();
        order.setId(200L);
        order.setStatus(OrderStatus.CREATED);
        Warehouse wh = new Warehouse(); wh.setId(50L);
        order.setWarehouse(wh);

        Product p = new Product(); p.setId(30L); p.setName("Prod30");
        SalesOrderLine line = SalesOrderLine.builder()
                .product(p)
                .qtyOrdered(2)
                .qtyReserved(0)
                .build();
        order.setLines(new ArrayList<>(List.of(line)));

        when(salesOrderRepository.findById(200L)).thenReturn(Optional.of(order));

        // inventoryService.smartReserve succeeds (no exception)
        doNothing().when(inventoryService).smartReserve(30L, 50L, 2, "SO200");

        SalesOrderResponseDto res = service.updateStatus(200L, "RESERVED");

        // order status should be RESERVED
        assertEquals("RESERVED", res.getStatus());
        verify(inventoryService).smartReserve(30L, 50L, 2, "SO200");
        verify(salesOrderRepository).save(order);
        // line should have qtyReserved set
        assertEquals(2, order.getLines().get(0).getQtyReserved());
    }

    @Test
    void updateStatus_reserve_partialReservation_keepsCreatedAndAddsWarnings() {
        SalesOrder order = new SalesOrder();
        order.setId(201L);
        order.setStatus(OrderStatus.CREATED);
        Warehouse wh = new Warehouse(); wh.setId(51L);
        order.setWarehouse(wh);

        Product p1 = new Product(); p1.setId(31L); p1.setName("P31");
        SalesOrderLine l1 = SalesOrderLine.builder().product(p1).qtyOrdered(1).qtyReserved(0).build();

        Product p2 = new Product(); p2.setId(32L); p2.setName("P32");
        SalesOrderLine l2 = SalesOrderLine.builder().product(p2).qtyOrdered(2).qtyReserved(0).build();

        order.setLines(new ArrayList<>(List.of(l1, l2)));
        when(salesOrderRepository.findById(201L)).thenReturn(Optional.of(order));

        // first product reserves ok, second throws StockUnavailableException
        doNothing().when(inventoryService).smartReserve(31L, 51L, 1, "SO201");
        doThrow(new StockUnavailableException("PO_CREATED:999")).when(inventoryService).smartReserve(32L, 51L, 2, "SO201");

        SalesOrderResponseDto res = service.updateStatus(201L, "RESERVED");

        // because not all lines reserved, status must remain CREATED
        assertEquals("CREATED", res.getStatus());
        assertFalse(res.getWarnings().isEmpty());
        verify(inventoryService).smartReserve(31L, 51L, 1, "SO201");
        verify(inventoryService).smartReserve(32L, 51L, 2, "SO201");
        verify(salesOrderRepository).save(order);
    }

    @Test
    void updateStatus_releaseReserved_releasesInventoryAndClearsQtyReserved() {
        SalesOrder order = new SalesOrder();
        order.setId(300L);
        order.setStatus(OrderStatus.RESERVED);
        Warehouse wh = new Warehouse(); wh.setId(60L);
        order.setWarehouse(wh);

        Product p = new Product(); p.setId(40L); p.setName("P40");
        SalesOrderLine line = SalesOrderLine.builder().product(p).qtyOrdered(2).qtyReserved(2).build();
        order.setLines(new ArrayList<>(List.of(line)));
        when(salesOrderRepository.findById(300L)).thenReturn(Optional.of(order));

        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setWarehouse(wh);
        inv.setQtyOnHand(10);
        inv.setQtyReserved(2);
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(40L, 60L)).thenReturn(Optional.of(inv));
        when(salesOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SalesOrderResponseDto res = service.updateStatus(300L, "CREATED");

        // verify inventory reserved decreased and saved
        assertEquals(0, inv.getQtyReserved());
        assertEquals(0, order.getLines().get(0).getQtyReserved());
        verify(inventoryRepository).save(inv);
        verify(salesOrderRepository).save(order);
    }

    @Test
    void updateStatus_invalidStatus_throwsBusinessException() {
        SalesOrder order = new SalesOrder();
        order.setId(400L);
        when(salesOrderRepository.findById(400L)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateStatus(400L, "UNKNOWN"));
        assertTrue(ex.getMessage().contains("Status invalide"));
    }

    @Test
    void delete_nonExistent_throws() {
        when(salesOrderRepository.existsById(999L)).thenReturn(false);
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.delete(999L));
        assertTrue(ex.getMessage().contains("SalesOrder non trouvée"));
    }

    @Test
    void shipOrder_success_shipsAndRecordsMovements() {
        SalesOrder order = new SalesOrder();
        order.setId(500L);
        Warehouse wh = new Warehouse(); wh.setId(70L);
        order.setWarehouse(wh);
        order.setStatus(OrderStatus.CREATED);

        Product p = new Product(); p.setId(80L);
        SalesOrderLine line = SalesOrderLine.builder().product(p).qtyOrdered(5).qtyReserved(3).build();
        order.setLines(new ArrayList<>(List.of(line)));

        when(salesOrderRepository.findById(500L)).thenReturn(Optional.of(order));

        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setWarehouse(wh);
        inv.setQtyOnHand(10);
        inv.setQtyReserved(3);
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(80L, 70L)).thenReturn(Optional.of(inv));
        when(inventoryMovementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(salesOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.shipOrder(500L, "TRACK-1");

        // inventory onHand decreased by qty shipped (3) and reserved decreased
        assertEquals(7, inv.getQtyOnHand());
        assertEquals(0, inv.getQtyReserved());
        // movement recorded
        ArgumentCaptor<InventoryMovement> mvCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(mvCaptor.capture());
        InventoryMovement mv = mvCaptor.getValue();
        assertEquals(MovementType.OUTBOUND, mv.getType());
        assertEquals(3, mv.getQty());
    }

    @Test
    void shipOrder_insufficientOnHand_adjustsQtyShipped() {
        SalesOrder order = new SalesOrder();
        order.setId(510L);
        Warehouse wh = new Warehouse(); wh.setId(71L);
        order.setWarehouse(wh);

        Product p = new Product(); p.setId(81L);
        SalesOrderLine line = SalesOrderLine.builder().product(p).qtyOrdered(5).qtyReserved(4).build();
        order.setLines(new ArrayList<>(List.of(line)));

        when(salesOrderRepository.findById(510L)).thenReturn(Optional.of(order));

        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setWarehouse(wh);
        inv.setQtyOnHand(2); // less than reserved
        inv.setQtyReserved(4);
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(81L, 71L)).thenReturn(Optional.of(inv));
        when(inventoryMovementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(salesOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.shipOrder(510L, "TR2");

        // shipped only 2 units (onHand)
        assertEquals(0, inv.getQtyOnHand());
        // reserved decreased by shipped 2 -> becomes 2
        assertEquals(2, inv.getQtyReserved());
        // movement recorded with qty 2
        ArgumentCaptor<InventoryMovement> mvCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(mvCaptor.capture());
        InventoryMovement mv = mvCaptor.getValue();
        assertEquals(2, mv.getQty());
    }

    @Test
    void shipOrder_orderNotFound_throws() {
        when(salesOrderRepository.findById(777L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.shipOrder(777L, "T"));
        assertTrue(ex.getMessage().contains("SalesOrder non trouvée"));
    }

    @Test
    void shipOrder_canceledOrder_throwsBusinessException() {
        SalesOrder order = new SalesOrder();
        order.setId(600L);
        order.setStatus(OrderStatus.CANCELED);
        when(salesOrderRepository.findById(600L)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.shipOrder(600L, "T"));
        assertTrue(ex.getMessage().contains("Impossible d'expédier une commande annulée"));
    }
}