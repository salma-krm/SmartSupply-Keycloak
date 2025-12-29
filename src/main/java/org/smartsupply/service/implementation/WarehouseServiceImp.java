package org.smartsupply.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.dto.request.WarehouseRequestDto;
import org.smartsupply.dto.response.UserResponseDto;
import org.smartsupply.dto.response.WarehouseDetailDto;
import org.smartsupply.dto.response.WarehouseSimpleDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.UserMapper;
import org.smartsupply.mapper.WarehouseMapper;
import org.smartsupply.model.entity.User;
import org.smartsupply.model.entity.Warehouse;
import org.smartsupply.repository.InventoryRepository;
import org.smartsupply.repository.WarehouseRepository;
import org.smartsupply.service.UserService;
import org.smartsupply.service.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WarehouseServiceImp implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final InventoryRepository inventoryRepository;
    private final WarehouseMapper warehouseMapper;

    @Override
    @Transactional
    public WarehouseSimpleDto createWarehouse(WarehouseRequestDto warehouseRequestDto) {
        log.info("Création d'un warehouse code={}", warehouseRequestDto.getCode());
        if (warehouseRequestDto.getCode() == null || warehouseRequestDto.getCode().isBlank()) {
            throw new BusinessException("Le code de l'entrepôt est obligatoire");
        }
        if (warehouseRepository.existsByCode(warehouseRequestDto.getCode())) {
            throw new DuplicateResourceException("Un entrepôt avec ce code existe déjà");
        }

        Warehouse warehouse = Warehouse.builder()
                .code(warehouseRequestDto.getCode())
                .name(warehouseRequestDto.getName())
                .active(warehouseRequestDto.getActive() == null ? Boolean.TRUE : warehouseRequestDto.getActive())
                .build();


        if (warehouseRequestDto.getManagerEmail() != null) {
            UserResponseDto managerResponseDto = userService.getUserByEmail(warehouseRequestDto.getManagerEmail());
             User  manager =   userMapper.toEntity(managerResponseDto);
            warehouse.setManager(manager);
        }

        Warehouse saved = warehouseRepository.save(warehouse);
        return warehouseMapper.toSimpleDto(saved);
    }

    @Override
    public List<WarehouseSimpleDto> getAllWarehouses() {
        log.info("Récupération de tous les entrepôts");
        List<Warehouse> list = warehouseRepository.findAll();
        return list.stream().map(warehouseMapper::toSimpleDto).collect(Collectors.toList());
    }

    @Override
    public WarehouseDetailDto getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse introuvable id=" + id));

        return warehouseMapper.toDetailDto(warehouse);
    }

    @Override
    @Transactional
    public WarehouseSimpleDto updateWarehouse(Long id, WarehouseRequestDto request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse introuvable id=" + id));

        if (request.getCode() != null && !request.getCode().equals(warehouse.getCode())) {
            if (warehouseRepository.existsByCode(request.getCode())) {
                throw new DuplicateResourceException("Un entrepôt avec ce code existe déjà");
            }
            warehouse.setCode(request.getCode());
        }

        if (request.getName() != null)warehouse.setName(request.getName());
        if (request.getActive() != null) warehouse.setActive(request.getActive());

        if (request.getManagerEmail() != null) {
            UserResponseDto managerResponseDto = userService.getUserByEmail(request.getManagerEmail());
            User  manager =   userMapper.toEntity(managerResponseDto);
            warehouse.setManager(manager);
        } else {
            warehouse.setManager(null);
        }

        Warehouse updated = warehouseRepository.save(warehouse);
        return warehouseMapper.toSimpleDto(updated);
    }

    @Override
    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse introuvable id=" + id));


        long invCount = inventoryRepository.findByWarehouseId(id).stream().count();
        if (invCount > 0) {
            throw new BusinessException("Impossible de supprimer un entrepôt contenant des stocks. Désactivez-le d'abord.");
        }

        warehouseRepository.delete(warehouse);
        log.info("Warehouse supprimé id={}", id);
    }

    @Override
    public boolean existsById(Long id) {
        return warehouseRepository.existsById(id);
    }
}