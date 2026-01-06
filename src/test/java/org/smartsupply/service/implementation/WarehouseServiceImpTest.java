package org.smartsupply.service.implementation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smartsupply.dto.request.WarehouseRequestDto;
import org.smartsupply.dto.response.UserResponseDto;
import org.smartsupply.dto.response.WarehouseDetailDto;
import org.smartsupply.dto.response.WarehouseSimpleDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.UserMapper;
import org.smartsupply.mapper.WarehouseMapper;
import org.smartsupply.model.entity.Inventory;
import org.smartsupply.model.entity.User;
import org.smartsupply.model.entity.Warehouse;
import org.smartsupply.repository.InventoryRepository;
import org.smartsupply.service.UserService;
import org.smartsupply.repository.WarehouseRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImpTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private WarehouseMapper warehouseMapper;

    @InjectMocks
    private WarehouseServiceImp service;

    @Test
    void createWarehouse_codeNull_throwsBusinessException() {
        WarehouseRequestDto req = new WarehouseRequestDto();
        req.setCode(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createWarehouse(req));
        assertTrue(ex.getMessage().contains("obligatoire"));
    }

    @Test
    void createWarehouse_duplicateCode_throwsDuplicateResourceException() {
        WarehouseRequestDto req = new WarehouseRequestDto();
        req.setCode("W1");
        when(warehouseRepository.existsByCode("W1")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> service.createWarehouse(req));
        assertTrue(ex.getMessage().contains("existe déjà"));
    }

    @Test
    void createWarehouse_withManager_savesAndReturnsDto() {
        WarehouseRequestDto req = new WarehouseRequestDto();
        req.setCode("W2");
        req.setName("Warehouse 2");
        req.setActive(true);
        req.setManagerEmail("m@example.com");

        when(warehouseRepository.existsByCode("W2")).thenReturn(false);

        UserResponseDto managerDto = new UserResponseDto();
        managerDto.setId(11L);
        managerDto.setEmail("m@example.com");
        when(userService.getUserByEmail("m@example.com")).thenReturn(managerDto);

        User manager = new User();
        manager.setEmail("m@example.com");
        when(userMapper.toEntity(managerDto)).thenReturn(manager);

        // Save returns warehouse with id
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(i -> {
            Warehouse w = i.getArgument(0);
            w.setId(5L);
            return w;
        });

        WarehouseSimpleDto outDto = new WarehouseSimpleDto();
        outDto.setId(5L);
        outDto.setCode("W2");
        when(warehouseMapper.toSimpleDto(any(Warehouse.class))).thenReturn(outDto);

        WarehouseSimpleDto res = service.createWarehouse(req);

        assertNotNull(res);
        assertEquals(5L, res.getId());
        assertEquals("W2", res.getCode());
        // verify interactions
        verify(userService).getUserByEmail("m@example.com");
        verify(userMapper).toEntity(managerDto);
        verify(warehouseRepository).save(any(Warehouse.class));
        verify(warehouseMapper).toSimpleDto(any(Warehouse.class));
    }

    @Test
    void getAllWarehouses_returnsMappedList() {
        Warehouse w1 = new Warehouse();
        w1.setId(1L);
        Warehouse w2 = new Warehouse();
        w2.setId(2L);
        when(warehouseRepository.findAll()).thenReturn(Arrays.asList(w1, w2));

        WarehouseSimpleDto d1 = new WarehouseSimpleDto();
        d1.setId(1L);
        WarehouseSimpleDto d2 = new WarehouseSimpleDto();
        d2.setId(2L);
        when(warehouseMapper.toSimpleDto(w1)).thenReturn(d1);
        when(warehouseMapper.toSimpleDto(w2)).thenReturn(d2);

        List<WarehouseSimpleDto> res = service.getAllWarehouses();
        assertEquals(2, res.size());
        assertEquals(1L, res.get(0).getId());
        verify(warehouseRepository).findAll();
    }

    @Test
    void getWarehouseById_found_returnsDetailDto() {
        Warehouse w = new Warehouse();
        w.setId(10L);
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(w));

        WarehouseDetailDto dto = new WarehouseDetailDto();
        dto.setId(10L);
        when(warehouseMapper.toDetailDto(w)).thenReturn(dto);

        WarehouseDetailDto res = service.getWarehouseById(10L);
        assertNotNull(res);
        assertEquals(10L, res.getId());
    }

    @Test
    void getWarehouseById_notFound_throws() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getWarehouseById(99L));
        assertTrue(ex.getMessage().contains("introuvable"));
    }

    @Test
    void updateWarehouse_changeCode_duplicate_throws() {
        Warehouse existing = new Warehouse();
        existing.setId(20L);
        existing.setCode("OLD");
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(existing));

        WarehouseRequestDto req = new WarehouseRequestDto();
        req.setCode("NEW");

        when(warehouseRepository.existsByCode("NEW")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> service.updateWarehouse(20L, req));
        assertTrue(ex.getMessage().contains("existe déjà"));
    }

    @Test
    void updateWarehouse_success_withManagerAndWithoutManager() {
        Warehouse existing = new Warehouse();
        existing.setId(21L);
        existing.setCode("C1");
        existing.setName("N1");
        existing.setActive(true);
        when(warehouseRepository.findById(21L)).thenReturn(Optional.of(existing));

        WarehouseRequestDto req = new WarehouseRequestDto();
        req.setCode("C1"); // same code -> no existsByCode check
        req.setName("NewName");
        req.setActive(false);
        req.setManagerEmail("mgr@example.com");

        UserResponseDto mgrDto = new UserResponseDto();
        mgrDto.setEmail("mgr@example.com");
        when(userService.getUserByEmail("mgr@example.com")).thenReturn(mgrDto);

        User mgr = new User();
        mgr.setEmail("mgr@example.com");
        when(userMapper.toEntity(mgrDto)).thenReturn(mgr);

        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(i -> i.getArgument(0));

        WarehouseSimpleDto out = new WarehouseSimpleDto();
        out.setId(21L);
        when(warehouseMapper.toSimpleDto(any(Warehouse.class))).thenReturn(out);

        WarehouseSimpleDto res = service.updateWarehouse(21L, req);
        assertEquals(21L, res.getId());
        assertEquals("NewName", existing.getName());
        assertFalse(existing.getActive());
        assertNotNull(existing.getManager());

        // now test removing manager
        WarehouseRequestDto req2 = new WarehouseRequestDto();
        req2.setManagerEmail(null);
        when(warehouseRepository.findById(21L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(i -> i.getArgument(0));
        when(warehouseMapper.toSimpleDto(any(Warehouse.class))).thenReturn(out);

        WarehouseSimpleDto res2 = service.updateWarehouse(21L, req2);
        assertNull(existing.getManager());
    }

    @Test
    void deleteWarehouse_withInventory_throwsBusinessException() {
        Warehouse w = new Warehouse();
        w.setId(30L);
        when(warehouseRepository.findById(30L)).thenReturn(Optional.of(w));

        // inventory repository returns a non-empty iterable -> count > 0
        when(inventoryRepository.findByWarehouseId(30L))
                .thenReturn(Arrays.asList(mock(Inventory.class)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteWarehouse(30L));
        assertTrue(ex.getMessage().contains("Impossible de supprimer"));
    }

    @Test
    void deleteWarehouse_success_deletes() {
        Warehouse w = new Warehouse();
        w.setId(31L);
        when(warehouseRepository.findById(31L)).thenReturn(Optional.of(w));
        when(inventoryRepository.findByWarehouseId(31L)).thenReturn(Collections.emptyList());

        service.deleteWarehouse(31L);

        verify(warehouseRepository).delete(w);
    }

    @Test
    void existsById_delegates() {
        when(warehouseRepository.existsById(77L)).thenReturn(true);
        assertTrue(service.existsById(77L));
    }
}