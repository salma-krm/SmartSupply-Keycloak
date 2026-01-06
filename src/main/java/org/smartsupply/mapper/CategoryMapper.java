package org.smartsupply.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.smartsupply.dto.request.CategoryRequestDto;
import org.smartsupply.dto.request.CategoryUpdateDto;
import org.smartsupply.dto.response.CategoryDetailResponseDto;
import org.smartsupply.dto.response.CategoryResponseDto;
import org.smartsupply.dto.response.CategorySimpleResponseDto;
import org.smartsupply.dto.response.ProductSummaryDto;
import org.smartsupply.model.entity.Category;
import org.smartsupply.model.entity.Product;

import java.math.BigDecimal;
import java.util.List;


@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "products", ignore = true)
    Category toEntity(CategoryRequestDto categoryRequestDto);

    @Mapping(target = "productCount", expression = "java(getProductCount(category))")
    CategoryResponseDto toResponseDto(Category category);

    @Mapping(target = "productCount", expression = "java(getProductCount(category))")
    @Mapping(target = "products", expression = "java(mapProductsToSummary(category.getProducts()))")
    CategoryDetailResponseDto toDetailResponseDto(Category category);

    CategorySimpleResponseDto toSimpleResponseDto(Category category);

    List<CategoryResponseDto> toResponseDtoList(List<Category> categories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "products", ignore = true)
    void updateEntityFromDto(CategoryUpdateDto categoryUpdateDto, @MappingTarget Category category);

    default Integer getProductCount(Category category) {
        return category.getProducts() != null ? category.getProducts().size() : 0;
    }

    default List<ProductSummaryDto> mapProductsToSummary(List<Product> products) {
        if (products == null) {
            return null;
        }
        return products.stream()
                .map(this::toProductSummary)
                .toList();
    }

    default ProductSummaryDto toProductSummary(Product product) {
        if (product == null) {
            return null;
        }
        return ProductSummaryDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .originalPrice(product.getOriginalPrice())
                .profit(product.getProfite())
                .sellingPrice(calculateSellingPrice(product))
                .unit(product.getUnit())
                .active(product.getActive())
                .build();
    }

    default BigDecimal calculateSellingPrice(Product product) {
        if (product.getOriginalPrice() == null || product.getProfite() == null) {
            return BigDecimal.ZERO;
        }
        return product.getOriginalPrice().add(product.getProfite());
    }
}