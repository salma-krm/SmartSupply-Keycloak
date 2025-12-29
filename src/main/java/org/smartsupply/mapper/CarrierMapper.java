package org.smartsupply.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.smartsupply.dto.request.CarrierRequestDto;
import org.smartsupply.dto.response.CarrierResponseDto;
import org.smartsupply.dto.response.CarrierSimpleDto;
import org.smartsupply.model.entity.Carrier;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CarrierMapper {

    Carrier toEntity(CarrierRequestDto carrierRequestDto);

    CarrierResponseDto toResponseDto(Carrier carrier);

    void updateEntityFromDto(CarrierRequestDto carrierRequestDto, @MappingTarget Carrier carrier);
    CarrierSimpleDto toSimpleDto(Carrier carrier);

    List<CarrierResponseDto> toResponseDtoList(List<Carrier> carriers);

}
