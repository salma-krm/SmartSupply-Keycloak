package org.smartsupply.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.smartsupply.dto.response.SalesOrderLineResponseDto;
import org.smartsupply.model.entity.SalesOrderLine;

@Mapper(componentModel = "spring")
public interface SalesOrderLineMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", expression = "java(line.getProduct()!=null? line.getProduct().getSku() : null)")
    @Mapping(target = "productName", expression = "java(line.getProduct()!=null? line.getProduct().getName() : null)")
    SalesOrderLineResponseDto toResponse(SalesOrderLine line);
}