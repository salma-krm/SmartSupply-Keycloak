package org.smartsupply.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.exception.StockUnavailableException;
import org.smartsupply.model.entity.*;
import org.smartsupply.model.enums.MovementType;
import org.smartsupply.repository.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceImpTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private InventoryMovementRepository movementRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private InventoryServiceImp service;

    @BeforeEach
    void setup() {

        lenient().when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(movementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(purchaseOrderRepository.save(any())).thenAnswer(i -> {
            PurchaseOrder po = i.getArgument(0);
            if (po != null && po.getId() == null) {
                ReflectionTestUtils.setField(po, "id", 99L);
            }
            return po;
        });
        lenient().when(supplierRepository.save(any())).thenAnswer(i -> {
            Supplier s = i.getArgument(0);
            if (s != null && s.getId() == null) {
                ReflectionTestUtils.setField(s, "id", 77L);
            }
            return s;
        });
    }


    @Test
    void ensureInventoryExists_productNotFound_throws() {

        when(productRepository.existsById(1L)).thenReturn(false);


        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.ensureInventoryExists(1L, 10L));
        assertTrue(ex.getMessage().contains("Product not found"));
    }

    @Test
    void ensureInventoryExists_warehouseNotFound_throws() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(warehouseRepository.existsById(10L)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.ensureInventoryExists(1L, 10L));
        assertTrue(ex.getMessage().contains("Warehouse not found"));
    }

    @Test
    void ensureInventoryExists_createsInventory_whenAbsent() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(warehouseRepository.existsById(10L)).thenReturn(true);
        when(inventoryRepository.existsByProductIdAndWarehouseId(1L, 10L)).thenReturn(false);

        Product p = Product.builder().id(1L).build();
        Warehouse w = Warehouse.builder().id(10L).build();
        when(productRepository.getReferenceById(1L)).thenReturn(p);
        when(warehouseRepository.getReferenceById(10L)).thenReturn(w);

        service.ensureInventoryExists(1L, 10L);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        Inventory saved = captor.getValue();
        assertEquals(p, saved.getProduct());
        assertEquals(w, saved.getWarehouse());
        assertEquals(0, saved.getQtyOnHand());
        assertEquals(0, saved.getQtyReserved());
    }

    // ================== inbound ==================
    @Test
    void inbound_increasesQtyAndSavesMovement() {
        Inventory inv = Inventory.builder().id(1L).qtyOnHand(5).qtyReserved(0).build();
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 10L))
                .thenReturn(Optional.of(inv));

        service.inbound(1L, 10L, 3, "REF-IN");

        assertEquals(8, inv.getQtyOnHand());
        verify(inventoryRepository).save(inv);

        ArgumentCaptor<InventoryMovement> mCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(mCaptor.capture());
        InventoryMovement mv = mCaptor.getValue();
        assertEquals(MovementType.INBOUND, mv.getType());
        assertEquals(3, mv.getQty());
        assertEquals("REF-IN", mv.getReference());
    }

    // ================== outbound ==================
    @Test
    void outbound_whenNotEnoughStock_throwsStockUnavailable() {
        Inventory inv = Inventory.builder().id(1L).qtyOnHand(5).qtyReserved(3).build();
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 10L))
                .thenReturn(Optional.of(inv));

        StockUnavailableException ex = assertThrows(StockUnavailableException.class,
                () -> service.outbound(1L, 10L, 3, "REF-OUT"));
        assertTrue(ex.getMessage().contains("Stock insuffisant"));
    }

    @Test
    void outbound_success_updatesQtyAndRecordsMovement() {
        Inventory inv = Inventory.builder().id(1L).qtyOnHand(10).qtyReserved(2).build();
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 10L))
                .thenReturn(Optional.of(inv));

        service.outbound(1L, 10L, 5, "REF-OUT");

        assertEquals(5, inv.getQtyOnHand());
        verify(inventoryRepository).save(inv);

        ArgumentCaptor<InventoryMovement> mCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(mCaptor.capture());
        InventoryMovement mv = mCaptor.getValue();
        assertEquals(MovementType.OUTBOUND, mv.getType());
        assertEquals(5, mv.getQty());
        assertEquals("REF-OUT", mv.getReference());
    }

    // ================== adjustment ==================
    @Test
    void adjustment_whenResultBelowReserved_throwsBusinessException() {
        Inventory inv = Inventory.builder().id(1L).qtyOnHand(5).qtyReserved(4).build();
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 10L))
                .thenReturn(Optional.of(inv));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.adjustment(1L, 10L, -2, "ADJ-REF"));
        assertTrue(ex.getMessage().contains("Ajustement invalide"));
    }

    @Test
    void adjustment_success_recordsMovement() {
        Inventory inv = Inventory.builder().id(1L).qtyOnHand(5).qtyReserved(2).build();
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 10L))
                .thenReturn(Optional.of(inv));

        service.adjustment(1L, 10L, 3, "ADJ-REF");

        assertEquals(8, inv.getQtyOnHand());
        verify(inventoryRepository).save(inv);

        ArgumentCaptor<InventoryMovement> mCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(mCaptor.capture());
        InventoryMovement mv = mCaptor.getValue();
        assertEquals(MovementType.ADJUSTMENT, mv.getType());
        assertEquals(3, mv.getQty());
        assertEquals("ADJ-REF", mv.getReference());
    }

    // ================== reserve ==================
    @Test
    void reserve_whenNotEnoughAvailable_throwsStockUnavailable() {
        Inventory inv = Inventory.builder().id(1L).qtyOnHand(5).qtyReserved(4).build();
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 10L))
                .thenReturn(Optional.of(inv));

        StockUnavailableException ex = assertThrows(StockUnavailableException.class,
                () -> service.reserve(1L, 10L, 2, "SRC", 3600));
        assertTrue(ex.getMessage().contains("Stock insuffisant pour réservation"));
    }

    @Test
    void reserve_success_incrementsReservedAndRecordsAdjustmentMovement() {
        Inventory inv = Inventory.builder().id(1L).qtyOnHand(10).qtyReserved(2).build();
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 10L))
                .thenReturn(Optional.of(inv));

        String token = service.reserve(1L, 10L, 3, "SRC", 3600);
        assertNotNull(token);
        assertEquals(5, inv.getQtyReserved());
        verify(inventoryRepository).save(inv);

        ArgumentCaptor<InventoryMovement> mCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(mCaptor.capture());
        InventoryMovement mv = mCaptor.getValue();
        assertEquals(MovementType.ADJUSTMENT, mv.getType());
        assertEquals(3, mv.getQty());
        assertEquals("SRC", mv.getReference());
    }

    // ================== transfer ==================
    @Test
    void transfer_sameSourceAndTarget_throwsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.transfer(1L, 10L, 10L, 1, "REF"));
        assertTrue(ex.getMessage().contains("Source and target warehouses must differ"));
    }

    @Test
    void transfer_insufficientSource_throwsBusinessException() {
        Inventory source = Inventory.builder().id(1L).qtyOnHand(5).qtyReserved(3).product(Product.builder().id(1L).build()).build();
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(source));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.transfer(1L, 10L, 20L, 3, "TREF"));
        assertTrue(ex.getMessage().contains("Stock insuffisant"));
    }

    @Test
    void transfer_success_createsAndMovesQty_betweenWarehouses() {
        Product product = Product.builder().id(1L).build();
        Warehouse sourceW = Warehouse.builder().id(10L).build();
        Warehouse targetW = Warehouse.builder().id(20L).build();

        Inventory source = Inventory.builder().id(1L).product(product).warehouse(sourceW).qtyOnHand(10).qtyReserved(0).build();
        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(source));

        when(inventoryRepository.findWithLockByProductIdAndWarehouseId(1L, 20L)).thenReturn(Optional.empty());
        when(warehouseRepository.getReferenceById(20L)).thenReturn(targetW);

        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> {
            Inventory inv = i.getArgument(0);
            if (inv.getId() == null) ReflectionTestUtils.setField(inv, "id", 55L);
            return inv;
        });

        service.transfer(1L, 10L, 20L, 4, "TREFF");

        assertEquals(6, source.getQtyOnHand());

        ArgumentCaptor<Inventory> invCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, atLeastOnce()).save(invCaptor.capture());
        List<Inventory> saved = invCaptor.getAllValues();
        assertTrue(saved.stream().anyMatch(i -> i.getWarehouse().getId().equals(20L)));

        verify(movementRepository, times(2)).save(any(InventoryMovement.class));
    }

    @Test
    void smartReserve_createsPO_when_notEnoughAnywhere() {
        when(inventoryRepository.findAvailableByProductIdAndWarehouseId(1L, 10L)).thenReturn(0);
        when(warehouseRepository.findAllIdsExcept(10L)).thenReturn(Collections.emptyList());

        Product product = Product.builder().id(1L).originalPrice(new BigDecimal("5.5")).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(supplierRepository.findAll()).thenReturn(Collections.emptyList());


        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(i -> {
            PurchaseOrder po = i.getArgument(0);
            if (po != null) ReflectionTestUtils.setField(po, "id", 1234L);
            return po;
        });

        StockUnavailableException ex = assertThrows(StockUnavailableException.class,
                () -> service.smartReserve(1L, 10L, 7, "SMREF"));
        assertTrue(ex.getMessage().contains("PO_CREATED:1234"));
    }


    @Test
    void getAvailable_returnsZeroWhenNull() {
        when(inventoryRepository.findAvailableByProductIdAndWarehouseId(1L, 10L)).thenReturn(null);
        Integer v = service.getAvailable(1L, 10L);
        assertEquals(0, v);
    }

    @Test
    void findWarehousesWithAvailable_delegatesToRepository() {
        List<Long> list = Arrays.asList(10L, 20L);
        when(inventoryRepository.findWarehouseIdsWithAvailable(1L)).thenReturn(list);
        List<Long> res = service.findWarehousesWithAvailable(1L);
        assertEquals(list, res);
    }

}