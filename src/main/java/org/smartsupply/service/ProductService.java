package org.smartsupply.service;

import org.smartsupply.dto.request.ProductRequestDto;
import org.smartsupply.dto.request.ProductUpdateDto;
import org.smartsupply.dto.response.ProductResponseDto;

import java.util.List;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto productRequestDto);
    List<ProductResponseDto> getAllProducts();
    List<ProductResponseDto> getActiveProducts();
    List<ProductResponseDto> getInactiveProducts();
    ProductResponseDto getProductById(Long id);
    ProductResponseDto getProductBySku(String sku);
    List<ProductResponseDto> getProductsByCategory(Long categoryId);
    ProductResponseDto updateProduct(Long id, ProductUpdateDto productUpdateDto);
    ProductResponseDto toggleProductStatus(Long id);
    void deleteProduct(Long id);
    List<ProductResponseDto> searchByName(String name);
    List<ProductResponseDto> searchBySku(String sku);
    List<ProductResponseDto> searchProducts(String keyword);
    boolean existsById(Long id);
    //deactivate product with  sku
    void deactivateProduct(String sku);
}