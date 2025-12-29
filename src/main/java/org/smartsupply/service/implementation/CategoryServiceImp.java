package org.smartsupply.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.dto.request.CategoryRequestDto;
import org.smartsupply.dto.request.CategoryUpdateDto;
import org.smartsupply.dto.response.CategoryDetailResponseDto;
import org.smartsupply.dto.response.CategoryResponseDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.CategoryMapper;
import org.smartsupply.model.entity.Category;
import org.smartsupply.repository.CategoryRepository;
import org.smartsupply.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImp implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto){
        log.info("Création d'une nouvelle catégorie: {}", categoryRequestDto.getName());
        if(categoryRepository.existsByName(categoryRequestDto.getName())){
            throw new DuplicateResourceException("Une catégorie avec ce nom existe déjà");
        }
        Category category = categoryMapper.toEntity(categoryRequestDto);
        Category savedCategory = categoryRepository.save(category);
        log.info("Catégorie créée avec succès. ID: {}", savedCategory.getId());

        return categoryMapper.toResponseDto(savedCategory);
    }

    @Override
    public List<CategoryResponseDto> getAllCategories(){
        log.info("Récupération de toutes les catégories");
        List<Category> categories = categoryRepository.findAll();
        log.info("{} catégorie(s) trouvée(s)", categories.size());

        return categoryMapper.toResponseDtoList(categories);
    }


    @Override
    public CategoryResponseDto getCategoryById(Long id){
        log.info("Recherche de la catégorie avec l'ID: {}", id);
        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'ID: " + id));

        return categoryMapper.toResponseDto(category);
    }



    @Override
    public CategoryDetailResponseDto getCategoryWithProducts(Long id) {
        log.info("Recherche de la catégorie avec produits. ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'ID: " + id));
        return categoryMapper.toDetailResponseDto(category);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryUpdateDto categoryUpdateDto) {
        log.info("Mise à jour de la catégorie avec l'ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'ID: " + id));

        if (categoryRepository.existsByNameAndIdNot(categoryUpdateDto.getName(), id)) {
            throw new DuplicateResourceException("Une autre catégorie utilise déjà ce nom");
        }

        categoryMapper.updateEntityFromDto(categoryUpdateDto, category);
        Category updatedCategory = categoryRepository.save(category);
        log.info("Catégorie mise à jour avec succès. ID: {}", updatedCategory.getId());

        return categoryMapper.toResponseDto(updatedCategory);
    }



    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Suppression de la catégorie avec l'ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'ID: " + id));

        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new BusinessException(
                    String.format("Impossible de supprimer la catégorie '%s' car elle contient %d produit(s)",
                            category.getName(), category.getProducts().size())
            );
        }

        categoryRepository.delete(category);
        log.info("Catégorie supprimée avec succès. ID: {}", id);
    }


    @Override
    public List<CategoryResponseDto> searchCategories(String name) {
        log.info("Recherche de catégories avec le nom: {}", name);
        List<Category> categories = categoryRepository.searchByName(name);
        log.info("{} catégorie(s) trouvée(s)", categories.size());
        return categoryMapper.toResponseDtoList(categories);
    }

    @Override
    public boolean existsById(Long id) {
        return categoryRepository.existsById(id);
    }

}
