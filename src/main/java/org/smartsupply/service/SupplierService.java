package org.smartsupply.service;

import org.smartsupply.dto.request.SupplierRequestDto;
import org.smartsupply.dto.request.SupplierUpdateDto;
import org.smartsupply.dto.response.SupplierResponseDto;
import org.smartsupply.dto.response.SupplierSimpleDto;

import java.util.List;

public interface SupplierService {
    SupplierResponseDto createSupplier(SupplierRequestDto dto);
    List<SupplierResponseDto> getAllSuppliers();
    SupplierResponseDto getSupplierById(Long id);
    SupplierResponseDto updateSupplier(Long id, SupplierUpdateDto dto);
    void deleteSupplier(Long id);
    List<SupplierSimpleDto> getAllSimple();
    List<SupplierResponseDto> search(String q);
    boolean existsById(Long id);
}