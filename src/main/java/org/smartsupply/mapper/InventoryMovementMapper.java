package org.smartsupply.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.smartsupply.dto.response.InventoryMovementDto;
import org.smartsupply.model.entity.InventoryMovement;

@Mapper(componentModel = "spring")
public interface InventoryMovementMapper {

    @Mapping(target = "inventoryId", source = "inventory.id")
    InventoryMovementDto toDto(InventoryMovement m);
}