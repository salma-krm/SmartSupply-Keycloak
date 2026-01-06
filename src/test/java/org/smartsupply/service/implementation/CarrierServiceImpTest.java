package org.smartsupply.service.implementation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smartsupply.dto.request.CarrierRequestDto;
import org.smartsupply.dto.response.CarrierResponseDto;
import org.smartsupply.dto.response.CarrierSimpleDto;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.CarrierMapper;
import org.smartsupply.model.entity.Carrier;
import org.smartsupply.repository.CarrierRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarrierServiceImpTest {

    @Mock
    private CarrierRepository carrierRepository;

    @Mock
    private CarrierMapper carrierMapper;

    @InjectMocks
    private CarrierServiceImp service;

    @Test
    void createCarrier_duplicateName_throws() {
        CarrierRequestDto req = new CarrierRequestDto();
        req.setName("TNT");

        when(carrierRepository.existsByName("TNT")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> service.createCarrier(req));
        assertTrue(ex.getMessage().toLowerCase().contains("exist"));
        verify(carrierRepository).existsByName("TNT");
        verifyNoMoreInteractions(carrierRepository);
    }

    @Test
    void createCarrier_success_savesAndReturnsDto() {
        CarrierRequestDto req = new CarrierRequestDto();
        req.setName("DHL");
        req.setPhone("123");

        when(carrierRepository.existsByName("DHL")).thenReturn(false);

        Carrier entity = new Carrier();
        when(carrierMapper.toEntity(req)).thenReturn(entity);

        Carrier saved = new Carrier();
        saved.setId(10L);
        saved.setName("DHL");
        when(carrierRepository.save(entity)).thenReturn(saved);

        CarrierResponseDto dto = new CarrierResponseDto();
        dto.setId(10L);
        when(carrierMapper.toResponseDto(saved)).thenReturn(dto);

        CarrierResponseDto res = service.createCarrier(req);

        assertNotNull(res);
        assertEquals(10L, res.getId());
        verify(carrierRepository).existsByName("DHL");
        verify(carrierMapper).toEntity(req);
        verify(carrierRepository).save(entity);
        verify(carrierMapper).toResponseDto(saved);
    }

    @Test
    void getAllCarriers_delegatesToMapper() {
        Carrier c1 = new Carrier(); c1.setId(1L);
        Carrier c2 = new Carrier(); c2.setId(2L);
        List<Carrier> list = Arrays.asList(c1, c2);
        when(carrierRepository.findAll()).thenReturn(list);

        CarrierResponseDto r1 = new CarrierResponseDto(); r1.setId(1L);
        CarrierResponseDto r2 = new CarrierResponseDto(); r2.setId(2L);
        when(carrierMapper.toResponseDtoList(list)).thenReturn(Arrays.asList(r1, r2));

        List<CarrierResponseDto> res = service.getAllCarriers();
        assertEquals(2, res.size());
        verify(carrierRepository).findAll();
        verify(carrierMapper).toResponseDtoList(list);
    }

    @Test
    void getCarrierById_found_returnsDto() {
        Carrier c = new Carrier(); c.setId(5L);
        when(carrierRepository.findById(5L)).thenReturn(Optional.of(c));

        CarrierResponseDto dto = new CarrierResponseDto(); dto.setId(5L);
        when(carrierMapper.toResponseDto(c)).thenReturn(dto);

        CarrierResponseDto res = service.getCarrierById(5L);
        assertEquals(5L, res.getId());
        verify(carrierRepository).findById(5L);
        verify(carrierMapper).toResponseDto(c);
    }

    @Test
    void getCarrierById_notFound_throws() {
        when(carrierRepository.findById(42L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getCarrierById(42L));
        assertTrue(ex.getMessage().contains("introuvable") || ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    void updateCarrier_success_updatesAndReturnsDto() {
        Carrier existing = new Carrier();
        existing.setId(11L);
        existing.setName("OldName");
        when(carrierRepository.findById(11L)).thenReturn(Optional.of(existing));

        CarrierRequestDto req = new CarrierRequestDto();
        req.setName("NewName");
        req.setPhone("000");

        // existsByNameAndIdNot should be called only if name changed; here it changes -> return false
        when(carrierRepository.existsByNameAndIdNot("NewName", 11L)).thenReturn(false);

        // carrierMapper.updateEntityFromDto is void; we don't need to stub it (Mockito allows void)
        when(carrierRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        CarrierResponseDto out = new CarrierResponseDto();
        out.setId(11L);
        when(carrierMapper.toResponseDto(existing)).thenReturn(out);

        CarrierResponseDto res = service.updateCarrier(11L, req);

        assertEquals(11L, res.getId());
        verify(carrierRepository).findById(11L);
        verify(carrierRepository).existsByNameAndIdNot("NewName", 11L);
        verify(carrierRepository).save(existing);
        verify(carrierMapper).toResponseDto(existing);
    }

    @Test
    void updateCarrier_duplicateName_throws() {
        Carrier existing = new Carrier();
        existing.setId(12L);
        existing.setName("X");
        when(carrierRepository.findById(12L)).thenReturn(Optional.of(existing));

        CarrierRequestDto req = new CarrierRequestDto();
        req.setName("Another");

        when(carrierRepository.existsByNameAndIdNot("Another", 12L)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> service.updateCarrier(12L, req));
        assertTrue(ex.getMessage().toLowerCase().contains("exist"));
    }

    @Test
    void updateCarrier_notFound_throws() {
        when(carrierRepository.findById(999L)).thenReturn(Optional.empty());
        CarrierRequestDto req = new CarrierRequestDto();
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.updateCarrier(999L, req));
        assertTrue(ex.getMessage().contains("introuvable") || ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    void deleteCarrier_success_deletes() {
        Carrier c = new Carrier(); c.setId(20L);
        when(carrierRepository.findById(20L)).thenReturn(Optional.of(c));

        service.deleteCarrier(20L);

        verify(carrierRepository).delete(c);
    }

    @Test
    void deleteCarrier_notFound_throws() {
        when(carrierRepository.findById(404L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.deleteCarrier(404L));
        assertTrue(ex.getMessage().contains("introuvable") || ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    void searchCarriers_delegatesToRepoAndMapper() {
        Carrier c = new Carrier(); c.setId(30L);
        when(carrierRepository.search("foo")).thenReturn(Collections.singletonList(c));

        CarrierResponseDto dto = new CarrierResponseDto(); dto.setId(30L);
        when(carrierMapper.toResponseDtoList(anyList())).thenReturn(Collections.singletonList(dto));

        List<CarrierResponseDto> res = service.searchCarriers("foo");
        assertEquals(1, res.size());
        verify(carrierRepository).search("foo");
        verify(carrierMapper).toResponseDtoList(anyList());
    }

    @Test
    void existsById_delegates() {
        when(carrierRepository.existsById(77L)).thenReturn(true);
        assertTrue(service.existsById(77L));
        verify(carrierRepository).existsById(77L);
    }

    @Test
    void getAllSimple_mapsToSimpleDtoList() {
        Carrier c1 = new Carrier(); c1.setId(1L);
        Carrier c2 = new Carrier(); c2.setId(2L);
        when(carrierRepository.findAll()).thenReturn(Arrays.asList(c1, c2));

        CarrierSimpleDto s1 = new CarrierSimpleDto(); s1.setId(1L);
        CarrierSimpleDto s2 = new CarrierSimpleDto(); s2.setId(2L);
        when(carrierMapper.toSimpleDto(c1)).thenReturn(s1);
        when(carrierMapper.toSimpleDto(c2)).thenReturn(s2);

        List<CarrierSimpleDto> res = service.getAllSimple();
        assertEquals(2, res.size());
        assertEquals(1L, res.get(0).getId());
        verify(carrierRepository).findAll();
        verify(carrierMapper).toSimpleDto(c1);
        verify(carrierMapper).toSimpleDto(c2);
    }
}