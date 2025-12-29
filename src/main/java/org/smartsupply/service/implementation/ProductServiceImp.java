package org.smartsupply.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.ProductMapper;
import org.smartsupply.dto.request.ProductRequestDto;
import org.smartsupply.dto.request.ProductUpdateDto;
import org.smartsupply.dto.response.ProductResponseDto;
import org.smartsupply.model.entity.Category;
import org.smartsupply.model.entity.Inventory;
import org.smartsupply.model.entity.Product;
import org.smartsupply.model.enums.OrderStatus;
import org.smartsupply.repository.CategoryRepository;
import org.smartsupply.repository.InventoryRepository;
import org.smartsupply.repository.ProductRepository;
import org.smartsupply.repository.SalesOrderLineRepository;
import org.smartsupply.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImp implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final SalesOrderLineRepository salesOrderLineRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        log.info("Création d'un nouveau produit: {} (SKU: {})",
                productRequestDto.getName(), productRequestDto.getSku());

        if (productRepository.existsBySku(productRequestDto.getSku())) {
            throw new DuplicateResourceException("Un produit avec ce SKU existe déjà");
        }

        Category category = categoryRepository.findByName(productRequestDto.getCategoryName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable avec l'ID: " + productRequestDto.getCategoryName()));

        Product product = productMapper.toEntity(productRequestDto);
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);
        log.info("Produit créé avec succès. ID: {}, SKU: {}",
                savedProduct.getId(), savedProduct.getSku());

        return productMapper.toResponseDto(savedProduct);
    }

    @Override
    public List<ProductResponseDto> getAllProducts() {
        log.info("Récupération de tous les produits");
        List<Product> products = productRepository.findAll();
        log.info("{} produit(s) trouvé(s)", products.size());
        return productMapper.toResponseDtoList(products);
    }

    @Override
    public List<ProductResponseDto> getActiveProducts() {
        log.info("Récupération des produits actifs");
        List<Product> products = productRepository.findByActiveTrue();
        log.info("{} produit(s) actif(s) trouvé(s)", products.size());
        return productMapper.toResponseDtoList(products);
    }

    @Override
    public List<ProductResponseDto> getInactiveProducts() {
        log.info("Récupération des produits inactifs");
        List<Product> products = productRepository.findByActiveFalse();
        log.info("{} produit(s) inactif(s) trouvé(s)", products.size());
        return productMapper.toResponseDtoList(products);
    }

    @Override
    public ProductResponseDto getProductById(Long id) {
        log.info("Recherche du produit avec l'ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'ID: " + id));
        return productMapper.toResponseDto(product);
    }

    @Override
    public ProductResponseDto getProductBySku(String sku) {
        log.info("Recherche du produit avec le SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec le SKU: " + sku));
        return productMapper.toResponseDto(product);
    }

    @Override
    public List<ProductResponseDto> getProductsByCategory(Long categoryId) {
        log.info("Récupération des produits de la catégorie: {}", categoryId);

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Catégorie introuvable avec l'ID: " + categoryId);
        }

        List<Product> products = productRepository.findByCategoryId(categoryId);
        log.info("{} produit(s) trouvé(s) pour la catégorie {}", products.size(), categoryId);
        return productMapper.toResponseDtoList(products);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductUpdateDto productUpdateDto) {
        log.info("Mise à jour du produit avec l'ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'ID: " + id));

        if (productRepository.existsBySkuAndIdNot(productUpdateDto.getSku(), id)) {
            throw new DuplicateResourceException("Un autre produit utilise déjà ce SKU");
        }

        Category category = categoryRepository.findByName(productUpdateDto.getCategoryName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable avec l'ID: " + productUpdateDto.getCategoryName()));

        productMapper.updateEntityFromDto(productUpdateDto, product);

        if (productUpdateDto.getActive() != null) {
            product.setActive(productUpdateDto.getActive());
        } else if (product.getActive() == null) {
            product.setActive(true);
        }
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        log.info("Produit mis à jour avec succès. ID: {}", updatedProduct.getId());

        return productMapper.toResponseDto(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponseDto toggleProductStatus(Long id) {
        log.info("Changement du statut du produit avec l'ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'ID: " + id));

        product.setActive(!product.getActive());
        Product updatedProduct = productRepository.save(product);

        log.info("Statut du produit changé. ID: {}, Nouveau statut: {}",
                updatedProduct.getId(), updatedProduct.getActive() ? "Actif" : "Inactif");

        return productMapper.toResponseDto(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Suppression du produit avec l'ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'ID: " + id));

        productRepository.delete(product);
        log.info("Produit supprimé avec succès. ID: {}", id);
    }

    @Override
    public List<ProductResponseDto> searchByName(String name) {
        log.info("Recherche de produits par nom: {}", name);
        List<Product> products = productRepository.searchByName(name);
        log.info("{} produit(s) trouvé(s)", products.size());
        return productMapper.toResponseDtoList(products);
    }

    @Override
    public List<ProductResponseDto> searchBySku(String sku) {
        log.info("Recherche de produits par SKU: {}", sku);
        List<Product> products = productRepository.searchBySku(sku);
        log.info("{} produit(s) trouvé(s)", products.size());
        return productMapper.toResponseDtoList(products);
    }

    @Override
    public List<ProductResponseDto> searchProducts(String keyword) {
        log.info("Recherche globale de produits: {}", keyword);
        List<Product> products = productRepository.searchProducts(keyword);
        log.info("{} produit(s) trouvé(s)", products.size());
        return productMapper.toResponseDtoList(products);
    }

    @Override
    public boolean existsById(Long id) {
        return productRepository.existsById(id);
    }

    @Override
    @Transactional
    public void deactivateProduct(String sku) {
        log.info("Désactivation du produit avec le SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec le SKU: " + sku));

       long activeOrderCount = salesOrderLineRepository.countByProduct_SkuAndSalesOrder_StatusIn(sku, List.of(OrderStatus.CREATED, OrderStatus.RESERVED));
        if(activeOrderCount > 0){
            throw new BusinessException("Le produit est présent dans des commandes en cours (CREATED).");
        }


        int totalReserved = inventoryRepository.findByProduct_Sku(sku)
                .stream()
                .mapToInt(Inventory::getQtyReserved)
                .sum();

        if (totalReserved > 0){
            throw new BusinessException("Le produit a des quantités réservées en stock.");
        }

        product.setActive(false);
        productRepository.save(product);

        log.info("Produit désactivé avec succès. SKU: {}", sku);
    }


}

