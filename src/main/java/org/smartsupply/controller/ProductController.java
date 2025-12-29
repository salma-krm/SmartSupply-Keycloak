package org.smartsupply.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smartsupply.annotation.RequireAuth;
import org.smartsupply.annotation.RequireRole;
import org.smartsupply.dto.request.ProductRequestDto;
import org.smartsupply.dto.request.ProductUpdateDto;
import org.smartsupply.dto.response.ProductResponseDto;
import org.smartsupply.model.enums.Role;
import org.smartsupply.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor

public class ProductController {

    private final ProductService productService;

    @PostMapping
    @RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody ProductRequestDto productRequestDto) {
        ProductResponseDto response = productService.createProduct(productRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireAuth
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/active")
    @RequireAuth
    public ResponseEntity<List<ProductResponseDto>> getActiveProducts() {
        List<ProductResponseDto> products = productService.getActiveProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/inactive")
    @RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<List<ProductResponseDto>> getInactiveProducts() {
        List<ProductResponseDto> products = productService.getInactiveProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @RequireAuth
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        ProductResponseDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/sku/{sku}")
    @RequireAuth
    public ResponseEntity<ProductResponseDto> getProductBySku(@PathVariable String sku) {
        ProductResponseDto product = productService.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/category/{categoryId}")
    //@RequireAuth
    public ResponseEntity<List<ProductResponseDto>> getProductsByCategory(@PathVariable Long categoryId) {
        List<ProductResponseDto> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    @RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateDto productUpdateDto) {
        ProductResponseDto response = productService.updateProduct(id, productUpdateDto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/toggle-status")
    @RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<ProductResponseDto> toggleProductStatus(@PathVariable Long id) {
        ProductResponseDto response = productService.toggleProductStatus(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/name")
    @RequireAuth
    public ResponseEntity<List<ProductResponseDto>> searchByName(@RequestParam String name) {
        List<ProductResponseDto> products = productService.searchByName(name);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search/sku")
    @RequireAuth
    public ResponseEntity<List<ProductResponseDto>> searchBySku(@RequestParam String sku) {
        List<ProductResponseDto> products = productService.searchBySku(sku);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    @RequireAuth
    public ResponseEntity<List<ProductResponseDto>> searchProducts(@RequestParam String keyword) {
        List<ProductResponseDto> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }



    @PatchMapping("/{sku}/deactivate")
    public ResponseEntity<Void> deactivateProduct(@Valid @PathVariable String sku) {
                    productService.deactivateProduct(sku);
        return ResponseEntity.ok().build();
    }
}