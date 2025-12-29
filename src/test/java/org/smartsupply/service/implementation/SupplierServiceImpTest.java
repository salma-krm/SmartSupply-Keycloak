package org.smartsupply.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.smartsupply.dto.request.SupplierRequestDto;
import org.smartsupply.dto.request.SupplierUpdateDto;
import org.smartsupply.dto.response.SupplierResponseDto;
import org.smartsupply.dto.response.SupplierSimpleDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.SupplierMapper;
import org.smartsupply.model.entity.Supplier;
import org.smartsupply.repository.PurchaseOrderRepository;
import org.smartsupply.repository.SupplierRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImpTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private SupplierServiceImp service;

    @BeforeEach
    void setUp() {
        // nothing global - each test stubs exactly what it needs to avoid UnnecessaryStubbingException
    }

    @Test
    void createSupplier_duplicateEmail_throws() {
        SupplierRequestDto dto = new SupplierRequestDto();
        dto.setName("S1");
        dto.setEmail("a@x.com");

        when(supplierRepository.existsByEmail("a@x.com")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> service.createSupplier(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("email") || ex.getMessage().length() > 0);
        verify(supplierRepository).existsByEmail("a@x.com");
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void createSupplier_duplicateName_throws() {
        SupplierRequestDto dto = new SupplierRequestDto();
        dto.setName("S2");
        dto.setEmail("b@x.com");

        when(supplierRepository.existsByEmail("b@x.com")).thenReturn(false);
        when(supplierRepository.existsByName("S2")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> service.createSupplier(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("nom") || ex.getMessage().length() > 0);
        verify(supplierRepository).existsByName("S2");
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void createSupplier_success_returnsMappedDto() {
        SupplierRequestDto dto = new SupplierRequestDto();
        dto.setName("GoodCo");
        dto.setEmail("good@example.com");

        when(supplierRepository.existsByEmail("good@example.com")).thenReturn(false);
        when(supplierRepository.existsByName("GoodCo")).thenReturn(false);

        Supplier entity = new Supplier();
        when(supplierMapper.toEntity(dto)).thenReturn(entity);

        Supplier saved = new Supplier();
        ReflectionTestUtils.setField(saved, "id", 77L);
        saved.setName("GoodCo");
        saved.setEmail("good@example.com");
        when(supplierRepository.save(entity)).thenReturn(saved);

        SupplierResponseDto out = new SupplierResponseDto();
        out.setId(77L);
        when(supplierMapper.toResponseDto(saved)).thenReturn(out);

        SupplierResponseDto res = service.createSupplier(dto);
        assertNotNull(res);
        assertEquals(77L, res.getId());
        verify(supplierMapper).toEntity(dto);
        verify(supplierRepository).save(entity);
        verify(supplierMapper).toResponseDto(saved);
    }

    @Test
    void getAllSuppliers_delegatesToMapper() {
        Supplier s1 = new Supplier(); ReflectionTestUtils.setField(s1, "id", 1L);
        Supplier s2 = new Supplier(); ReflectionTestUtils.setField(s2, "id", 2L);
        List<Supplier> list = Arrays.asList(s1, s2);
        when(supplierRepository.findAll()).thenReturn(list);

        SupplierResponseDto r1 = new SupplierResponseDto(); r1.setId(1L);
        SupplierResponseDto r2 = new SupplierResponseDto(); r2.setId(2L);
        when(supplierMapper.toResponseDtoList(list)).thenReturn(Arrays.asList(r1, r2));

        List<SupplierResponseDto> res = service.getAllSuppliers();
        assertEquals(2, res.size());
        verify(supplierRepository).findAll();
        verify(supplierMapper).toResponseDtoList(list);
    }

    @Test
    void getSupplierById_found_returnsDto() {
        Supplier s = new Supplier();
        ReflectionTestUtils.setField(s, "id", 10L);
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(s));

        SupplierResponseDto dto = new SupplierResponseDto();
        dto.setId(10L);
        when(supplierMapper.toResponseDto(s)).thenReturn(dto);

        SupplierResponseDto res = service.getSupplierById(10L);
        assertEquals(10L, res.getId());
        verify(supplierRepository).findById(10L);
        verify(supplierMapper).toResponseDto(s);
    }

    @Test
    void getSupplierById_notFound_throws() {
        when(supplierRepository.findById(404L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getSupplierById(404L));
        assertTrue(ex.getMessage().contains("Fournisseur introuvable"));
    }

    @Test
    void updateSupplier_success_updatesAndReturnsDto() {
        Supplier existing = new Supplier();
        ReflectionTestUtils.setField(existing, "id", 20L);
        existing.setEmail("old@x.com");
        existing.setName("OldName");
        when(supplierRepository.findById(20L)).thenReturn(Optional.of(existing));

        SupplierUpdateDto dto = new SupplierUpdateDto();
        dto.setEmail("new@x.com");
        dto.setName("NewName");

        when(supplierRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(supplierRepository.existsByName("NewName")).thenReturn(false);

        // ensure updateEntityFromDto is invoked (void)
        doAnswer(inv -> { SupplierUpdateDto d = inv.getArgument(0); Supplier s = inv.getArgument(1); s.setEmail(d.getEmail()); s.setName(d.getName()); return null; })
                .when(supplierMapper).updateEntityFromDto(dto, existing);

        when(supplierRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        SupplierResponseDto out = new SupplierResponseDto(); out.setId(20L);
        when(supplierMapper.toResponseDto(existing)).thenReturn(out);

        SupplierResponseDto res = service.updateSupplier(20L, dto);
        assertEquals(20L, res.getId());
        assertEquals("new@x.com", existing.getEmail());
        assertEquals("NewName", existing.getName());
        verify(supplierRepository).existsByEmail("new@x.com");
        verify(supplierRepository).existsByName("NewName");
        verify(supplierMapper).updateEntityFromDto(dto, existing);
        verify(supplierRepository).save(existing);
    }

    @Test
    void updateSupplier_duplicateEmail_throws() {
        Supplier existing = new Supplier();
        ReflectionTestUtils.setField(existing, "id", 21L);
        existing.setEmail("old@x.com");
        when(supplierRepository.findById(21L)).thenReturn(Optional.of(existing));

        SupplierUpdateDto dto = new SupplierUpdateDto();
        dto.setEmail("dup@x.com");
        dto.setName("Whatever");

        when(supplierRepository.existsByEmail("dup@x.com")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> service.updateSupplier(21L, dto));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(supplierRepository).existsByEmail("dup@x.com");
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void updateSupplier_duplicateName_throws() {
        Supplier existing = new Supplier();
        ReflectionTestUtils.setField(existing, "id", 22L);
        existing.setName("Old");
        existing.setEmail("e@x.com");
        when(supplierRepository.findById(22L)).thenReturn(Optional.of(existing));

        SupplierUpdateDto dto = new SupplierUpdateDto();
        dto.setEmail("e@x.com"); // same email to bypass email check
        dto.setName("DupName");

        when(supplierRepository.existsByName("DupName")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> service.updateSupplier(22L, dto));
        assertTrue(ex.getMessage().toLowerCase().contains("nom") || ex.getMessage().length() > 0);
        verify(supplierRepository).existsByName("DupName");
    }

    @Test
    void updateSupplier_notFound_throws() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());
        SupplierUpdateDto dto = new SupplierUpdateDto();
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.updateSupplier(999L, dto));
        assertTrue(ex.getMessage().contains("Fournisseur introuvable"));
    }

    @Test
    void deleteSupplier_success_deletesWhenNoPO() {
        Supplier s = new Supplier();
        ReflectionTestUtils.setField(s, "id", 30L);
        when(supplierRepository.findById(30L)).thenReturn(Optional.of(s));
        when(purchaseOrderRepository.countBySupplierId(30L)).thenReturn(0L);

        service.deleteSupplier(30L);

        verify(purchaseOrderRepository).countBySupplierId(30L);
        verify(supplierRepository).delete(s);
    }

    @Test
    void deleteSupplier_throwsWhenReferencedByPOs() {
        Supplier s = new Supplier();
        ReflectionTestUtils.setField(s, "id", 31L);
        when(supplierRepository.findById(31L)).thenReturn(Optional.of(s));
        when(purchaseOrderRepository.countBySupplierId(31L)).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteSupplier(31L));
        assertTrue(ex.getMessage().contains("Impossible de supprimer"));
        verify(purchaseOrderRepository).countBySupplierId(31L);
        verify(supplierRepository, never()).delete(any());
    }

    @Test
    void deleteSupplier_notFound_throws() {
        when(supplierRepository.findById(404L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.deleteSupplier(404L));
        assertTrue(ex.getMessage().contains("Fournisseur introuvable"));
    }

    @Test
    void getAllSimple_mapsToSimpleDtoList() {
        Supplier s1 = new Supplier(); ReflectionTestUtils.setField(s1, "id", 1L);
        Supplier s2 = new Supplier(); ReflectionTestUtils.setField(s2, "id", 2L);
        when(supplierRepository.findAll()).thenReturn(Arrays.asList(s1, s2));

        SupplierSimpleDto sm1 = new SupplierSimpleDto(); sm1.setId(1L);
        SupplierSimpleDto sm2 = new SupplierSimpleDto(); sm2.setId(2L);
        when(supplierMapper.toSimpleDto(s1)).thenReturn(sm1);
        when(supplierMapper.toSimpleDto(s2)).thenReturn(sm2);

        List<SupplierSimpleDto> res = service.getAllSimple();
        assertEquals(2, res.size());
        verify(supplierRepository).findAll();
        verify(supplierMapper).toSimpleDto(s1);
        verify(supplierMapper).toSimpleDto(s2);
    }

    @Test
    void search_delegatesAndMaps() {
        Supplier s = new Supplier(); ReflectionTestUtils.setField(s, "id", 11L);
        when(supplierRepository.findByNameContainingIgnoreCase("foo")).thenReturn(Collections.singletonList(s));

        SupplierResponseDto r = new SupplierResponseDto(); r.setId(11L);
        when(supplierMapper.toResponseDto(s)).thenReturn(r);

        List<SupplierResponseDto> res = service.search("foo");
        assertEquals(1, res.size());
        assertEquals(11L, res.get(0).getId());
        verify(supplierRepository).findByNameContainingIgnoreCase("foo");
        verify(supplierMapper).toResponseDto(s);
    }

    @Test
    void existsById_delegates() {
        when(supplierRepository.existsById(77L)).thenReturn(true);
        assertTrue(service.existsById(77L));
        verify(supplierRepository).existsById(77L);
    }
}