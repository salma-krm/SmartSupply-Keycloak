package org.smartsupply.service.implementation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smartsupply.dto.request.CategoryRequestDto;
import org.smartsupply.dto.request.CategoryUpdateDto;
import org.smartsupply.dto.response.CategoryResponseDto;
import org.smartsupply.dto.response.CategoryDetailResponseDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.CategoryMapper;
import org.smartsupply.model.entity.Category;
import org.smartsupply.repository.CategoryRepository;
import org.smartsupply.model.entity.Product;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImpTest {

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImp service;

    @Test
    void createCategory_duplicate_throws() {
        CategoryRequestDto req = new CategoryRequestDto();
        req.setName("C1");
        when(categoryRepository.existsByName("C1")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> service.createCategory(req));
        assertTrue(ex.getMessage().contains("existe déjà"));
    }

    @Test
    void createCategory_success_returnsDto() {
        CategoryRequestDto req = new CategoryRequestDto();
        req.setName("C2");

        Category entity = new Category();
        when(categoryMapper.toEntity(req)).thenReturn(entity);

        Category saved = new Category();
        saved.setId(10L);
        when(categoryRepository.save(entity)).thenReturn(saved);

        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setId(10L);
        when(categoryMapper.toResponseDto(saved)).thenReturn(dto);

        CategoryResponseDto res = service.createCategory(req);
        assertEquals(10L, res.getId());
        verify(categoryRepository).save(entity);
    }

    @Test
    void getCategoryById_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getCategoryById(99L));
    }

    @Test
    void getCategoryWithProducts_returnsDetail() {
        Category c = new Category();
        c.setId(5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(c));

        CategoryDetailResponseDto detail = new CategoryDetailResponseDto();
        detail.setId(5L);
        when(categoryMapper.toDetailResponseDto(c)).thenReturn(detail);

        CategoryDetailResponseDto res = service.getCategoryWithProducts(5L);
        assertEquals(5L, res.getId());
    }

    @Test
    void updateCategory_duplicateName_throws() {
        CategoryUpdateDto dto = new CategoryUpdateDto();
        dto.setName("NEW");
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(new Category()));
        when(categoryRepository.existsByNameAndIdNot("NEW", 7L)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> service.updateCategory(7L, dto));
        assertTrue(ex.getMessage().contains("Une autre catégorie"));
    }

    @Test
    void deleteCategory_withProducts_throwsBusiness() {
        Category c = new Category();
        c.setId(11L);
        c.setName("C");
        c.setProducts(Arrays.asList(new Product()));
        when(categoryRepository.findById(11L)).thenReturn(Optional.of(c));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteCategory(11L));
        assertTrue(ex.getMessage().contains("Impossible de supprimer"));
    }

    @Test
    void deleteCategory_success_deletes() {
        Category c = new Category();
        c.setId(12L);
        when(categoryRepository.findById(12L)).thenReturn(Optional.of(c));

        service.deleteCategory(12L);

        verify(categoryRepository).delete(c);
    }

    @Test
    void searchCategories_delegates() {
        when(categoryRepository.searchByName("k")).thenReturn(Collections.emptyList());
        service.searchCategories("k");
        verify(categoryRepository).searchByName("k");
    }

    @Test
    void existsById_delegates() {
        when(categoryRepository.existsById(55L)).thenReturn(true);
        assertTrue(service.existsById(55L));
    }
}