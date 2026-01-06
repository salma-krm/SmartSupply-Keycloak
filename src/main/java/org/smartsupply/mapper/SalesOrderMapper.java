package org.smartsupply.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.smartsupply.dto.response.SalesOrderResponseDto;
import org.smartsupply.model.entity.SalesOrder;

@Mapper(componentModel = "spring", uses = {SalesOrderLineMapper.class})
public interface SalesOrderMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", expression = "java(order.getClient()!=null? order.getClient().getFirstName()+\" \"+order.getClient().getLastName(): null)")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseName", expression = "java(order.getWarehouse()!=null? order.getWarehouse().getName(): null)")
    SalesOrderResponseDto toResponse(SalesOrder order);
}