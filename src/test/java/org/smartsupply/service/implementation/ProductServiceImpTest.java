package org.smartsupply.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smartsupply.dto.request.ProductRequestDto;
import org.smartsupply.dto.response.ProductResponseDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.ProductMapper;
import org.smartsupply.model.entity.Category;
import org.smartsupply.model.entity.Inventory;
import org.smartsupply.model.entity.Product;
import org.smartsupply.repository.CategoryRepository;
import org.smartsupply.repository.InventoryRepository;
import org.smartsupply.repository.ProductRepository;
import org.smartsupply.repository.SalesOrderLineRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImpTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private SalesOrderLineRepository salesOrderLineRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductServiceImp service;

    @BeforeEach
    void setup() {
        // nothing global — stub only in each test to avoid unnecessary stubbing
    }

    @Test
    void createProduct_duplicateSku_throws() {
        ProductRequestDto req = new ProductRequestDto();
        req.setSku("SKU1");
        when(productRepository.existsBySku("SKU1")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> service.createProduct(req));
        assertTrue(ex.getMessage().contains("SKU existe déjà") || ex.getMessage().length() > 0);
        verify(productRepository).existsBySku("SKU1");
    }

    @Test
    void createProduct_categoryNotFound_throws() {
        ProductRequestDto req = new ProductRequestDto();
        req.setSku("SKU1");
        req.setCategoryName("CAT_A");
        when(productRepository.existsBySku("SKU1")).thenReturn(false);
        when(categoryRepository.findByName("CAT_A")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.createProduct(req));
        assertTrue(ex.getMessage().contains("Catégorie introuvable"));
        verify(categoryRepository).findByName("CAT_A");
    }

    @Test
    void createProduct_success_returnsDto() {
        ProductRequestDto req = new ProductRequestDto();
        req.setSku("SKU_OK");
        req.setName("Prod");
        req.setCategoryName("CAT_OK");

        when(productRepository.existsBySku("SKU_OK")).thenReturn(false);

        Category cat = new Category();
        cat.setId(5L);
        when(categoryRepository.findByName("CAT_OK")).thenReturn(Optional.of(cat));

        Product entity = new Product();
        when(productMapper.toEntity(req)).thenReturn(entity);

        Product saved = new Product();
        saved.setId(99L);
        saved.setSku("SKU_OK");
        when(productRepository.save(entity)).thenReturn(saved);

        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(99L);
        when(productMapper.toResponseDto(saved)).thenReturn(dto);

        ProductResponseDto res = service.createProduct(req);

        assertNotNull(res);
        assertEquals(99L, res.getId());
        verify(productRepository).save(entity);
        verify(productMapper).toResponseDto(saved);
    }

    @Test
    void deactivateProduct_whenActiveOrders_throwsBusinessException() {
        String sku = "S1";
        Product p = new Product();
        p.setSku(sku);
        when(productRepository.findBySku(sku)).thenReturn(Optional.of(p));
        when(salesOrderLineRepository.countByProduct_SkuAndSalesOrder_StatusIn(eq(sku), anyList()))
                .thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deactivateProduct(sku));
        assertTrue(ex.getMessage().contains("commandes en cours"));
    }

    @Test
    void deactivateProduct_whenReservedInInventory_throwsBusinessException() {
        String sku = "S2";
        Product p = new Product();
        p.setSku(sku);
        when(productRepository.findBySku(sku)).thenReturn(Optional.of(p));
        when(salesOrderLineRepository.countByProduct_SkuAndSalesOrder_StatusIn(eq(sku), anyList()))
                .thenReturn(0L);

        Inventory inv1 = new Inventory();
        inv1.setQtyReserved(3);
        when(inventoryRepository.findByProduct_Sku(sku)).thenReturn(Arrays.asList(inv1));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deactivateProduct(sku));
        assertTrue(ex.getMessage().contains("quantités réservées"));
    }

    @Test
    void deactivateProduct_success_disablesProduct() {
        String sku = "S3";
        Product p = new Product();
        p.setSku(sku);
        p.setActive(true);
        when(productRepository.findBySku(sku)).thenReturn(Optional.of(p));
        when(salesOrderLineRepository.countByProduct_SkuAndSalesOrder_StatusIn(eq(sku), anyList()))
                .thenReturn(0L);
        when(inventoryRepository.findByProduct_Sku(sku)).thenReturn(Collections.emptyList());
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        service.deactivateProduct(sku);

        assertFalse(p.getActive());
        verify(productRepository).save(p);
    }

    @Test
    void getProductById_notFound_throws() {
        when(productRepository.findById(123L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getProductById(123L));
    }

    @Test
    void getProductBySku_notFound_throws() {
        when(productRepository.findBySku("NONE")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getProductBySku("NONE"));
    }

    @Test
    void getProductsByCategory_categoryNotFound_throws() {
        when(categoryRepository.existsById(77L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.getProductsByCategory(77L));
    }

    @Test
    void existsById_delegates() {
        when(productRepository.existsById(5L)).thenReturn(true);
        assertTrue(service.existsById(5L));
    }
}