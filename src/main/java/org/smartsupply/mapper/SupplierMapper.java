package org.smartsupply.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.smartsupply.dto.request.SupplierRequestDto;
import org.smartsupply.dto.request.SupplierUpdateDto;
import org.smartsupply.dto.response.SupplierResponseDto;
import org.smartsupply.dto.response.SupplierSimpleDto;
import org.smartsupply.model.entity.Supplier;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplierMapper {
    Supplier toEntity(SupplierRequestDto dto);
    SupplierResponseDto toResponseDto(Supplier supplier);
    SupplierSimpleDto toSimpleDto(Supplier supplier);
    List<SupplierResponseDto> toResponseDtoList(List<Supplier> suppliers);
    void updateEntityFromDto(SupplierUpdateDto dto, @MappingTarget Supplier supplier);
}