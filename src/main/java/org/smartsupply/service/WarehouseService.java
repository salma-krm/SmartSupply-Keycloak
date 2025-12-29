package org.smartsupply.service;

import org.smartsupply.dto.request.WarehouseRequestDto;
import org.smartsupply.dto.response.WarehouseDetailDto;
import org.smartsupply.dto.response.WarehouseSimpleDto;

import java.util.List;

public interface WarehouseService {

    WarehouseSimpleDto createWarehouse(WarehouseRequestDto request);

    List<WarehouseSimpleDto> getAllWarehouses();

    WarehouseDetailDto getWarehouseById(Long id);

    WarehouseSimpleDto updateWarehouse(Long id, WarehouseRequestDto request);

    void deleteWarehouse(Long id);

    boolean existsById(Long id);
}