package org.smartsupply.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.dto.request.CarrierRequestDto;
import org.smartsupply.dto.response.CarrierResponseDto;
import org.smartsupply.dto.response.CarrierSimpleDto;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.CarrierMapper;
import org.smartsupply.model.entity.Carrier;
import org.smartsupply.repository.CarrierRepository;
import org.smartsupply.service.CarrierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarrierServiceImp implements CarrierService {

    private final CarrierRepository carrierRepository;
    private final CarrierMapper carrierMapper;

    @Override
    @Transactional
    public CarrierResponseDto createCarrier(CarrierRequestDto carrierRequestDto){
        log.info("Creating a new carrier: {}", carrierRequestDto.getName());

        if(carrierRepository.existsByName(carrierRequestDto.getName())){
            throw new DuplicateResourceException("Un transporteur avec ce nom existe deja  ");
        }

       Carrier carrier = carrierMapper.toEntity(carrierRequestDto);
       Carrier savedCarrier = carrierRepository.save(carrier);

        log.info("Carrier created successfully. ID: {}", savedCarrier.getId());

        return carrierMapper.toResponseDto(savedCarrier);

    }

    @Override
    public List<CarrierResponseDto> getAllCarriers() {
        List<Carrier> list = carrierRepository.findAll();
        return carrierMapper.toResponseDtoList(list);
    }

    @Override
    public CarrierResponseDto getCarrierById(Long id) {
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transporteur introuvable id=" + id));
        return carrierMapper.toResponseDto(carrier);
    }


    @Override
    @Transactional
    public CarrierResponseDto updateCarrier(Long id , CarrierRequestDto dto){
    Carrier carrier = carrierRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Transporteur introuvable id=" +id));

    if(!carrier.getName().equals(dto.getName()) && carrierRepository.existsByNameAndIdNot(dto.getName(),id)){
        throw new DuplicateResourceException("Un transporteur avec ce nom existe deja ");
    }

        carrierMapper.updateEntityFromDto(dto, carrier);
        Carrier updated = carrierRepository.save(carrier);
        return carrierMapper.toResponseDto(updated);

    }

    @Override
    @Transactional
    public void deleteCarrier(Long id) {
        Carrier c = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transporteur introuvable id=" + id));
        // Optional: check references (shipments) before delete, here assume cascade safe
        carrierRepository.delete(c);
        log.info("Carrier deleted id={}", id);
    }

    @Override
    public List<CarrierResponseDto> searchCarriers(String keyword) {
        List<Carrier> results = carrierRepository.search(keyword);
        return carrierMapper.toResponseDtoList(results);
    }

    @Override
    public boolean existsById(Long id) {
        return carrierRepository.existsById(id);
    }

    @Override
    public List<CarrierSimpleDto> getAllSimple() {
        return carrierRepository.findAll().stream().map(carrierMapper::toSimpleDto).collect(Collectors.toList());
    }


}
