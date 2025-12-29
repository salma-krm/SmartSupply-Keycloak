package org.smartsupply.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smartsupply.annotation.RequireAuth;
import org.smartsupply.annotation.RequireRole;
import org.smartsupply.dto.request.CategoryRequestDto;
import org.smartsupply.dto.request.CategoryUpdateDto;
import org.smartsupply.dto.response.CategoryDetailResponseDto;
import org.smartsupply.dto.response.CategoryResponseDto;
import org.smartsupply.model.enums.Role;
import org.smartsupply.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<CategoryResponseDto> createCategory(
            @Valid @RequestBody CategoryRequestDto categoryRequestDto) {
        CategoryResponseDto response = categoryService.createCategory(categoryRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireAuth
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        List<CategoryResponseDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @RequireAuth
    public ResponseEntity<CategoryResponseDto> getCategoryById(@PathVariable Long id) {
        CategoryResponseDto category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/{id}/details")
    @RequireAuth
    public ResponseEntity<CategoryDetailResponseDto> getCategoryWithProducts(@PathVariable Long id) {
        CategoryDetailResponseDto category = categoryService.getCategoryWithProducts(id);
        return ResponseEntity.ok(category);
    }

    @PutMapping("/{id}")
    @RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateDto categoryUpdateDto) {
        CategoryResponseDto response = categoryService.updateCategory(id, categoryUpdateDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @RequireAuth
    public ResponseEntity<List<CategoryResponseDto>> searchCategories(
            @RequestParam String name) {
        List<CategoryResponseDto> categories = categoryService.searchCategories(name);
        return ResponseEntity.ok(categories);
    }
}