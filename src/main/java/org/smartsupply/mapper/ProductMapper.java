package org.smartsupply.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.smartsupply.dto.request.ProductRequestDto;
import org.smartsupply.dto.request.ProductUpdateDto;
import org.smartsupply.dto.response.ProductResponseDto;
import org.smartsupply.dto.response.ProductSummaryDto;
import org.smartsupply.model.entity.Product;

import java.math.BigDecimal;
import java.util.List;
@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)

    @Mapping(target = "profite", source = "profit")
    Product toEntity(ProductRequestDto productRequestDto);

    @Mapping(target = "category", source = "category")

    @Mapping(target = "profit", source = "profite")
    @Mapping(target = "sellingPrice", expression = "java(calculateSellingPrice(product))")
    ProductResponseDto toResponseDto(Product product);

    @Mapping(target = "profit", source = "profite")
    @Mapping(target = "sellingPrice", expression = "java(calculateSellingPrice(product))")
    ProductSummaryDto toSummaryDto(Product product);

    List<ProductResponseDto> toResponseDtoList(List<Product> products);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
 
    @Mapping(target = "profite", source = "profit")
    void updateEntityFromDto(ProductUpdateDto productUpdateDto, @MappingTarget Product product);

    default BigDecimal calculateSellingPrice(Product product) {
        if (product.getOriginalPrice() == null || product.getProfite() == null) {
            return BigDecimal.ZERO;
        }
        return product.getOriginalPrice().add(product.getProfite());
    }
}
