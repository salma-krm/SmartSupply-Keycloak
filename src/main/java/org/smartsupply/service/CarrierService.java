package org.smartsupply.service;

import org.smartsupply.dto.request.CarrierRequestDto;
import org.smartsupply.dto.response.CarrierResponseDto;
import org.smartsupply.dto.response.CarrierSimpleDto;

import java.util.List;

public interface CarrierService {

    CarrierResponseDto createCarrier(CarrierRequestDto carrierRequestDto );
    List<CarrierResponseDto> getAllCarriers();
    CarrierResponseDto getCarrierById(Long id);
    CarrierResponseDto updateCarrier(Long id, CarrierRequestDto dto);
    void deleteCarrier(Long id);
    List<CarrierResponseDto> searchCarriers(String q);
    boolean existsById(Long id);
    List<CarrierSimpleDto> getAllSimple();
}
