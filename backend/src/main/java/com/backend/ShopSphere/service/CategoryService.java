package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.CategoryRequest;
import com.backend.ShopSphere.dto.CategoryResponse;
import com.backend.ShopSphere.dto.ProductResponse;
import com.backend.ShopSphere.entity.Category;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.mapper.ProductAndCategoryMapper;
import com.backend.ShopSphere.repository.CategoryRepository;
import com.backend.ShopSphere.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductAndCategoryMapper productAndCategoryMapper;

    public CategoryResponse createCategory(CategoryRequest request){
        log.debug("Request received to create new category.");
        log.debug("Request details: {}", request);

        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .level(1)
                .build();

        if(request.getParentCategoryId() != null){
            Category parentCategory = categoryRepository.findById(request.getParentCategoryId()).orElseThrow(
                    () -> new ResourceNotFoundException("Parent category not found")
            );
            parentCategory.addSubCategory(category);
        } else {
            category.setPath(request.getName());
        }
        Category saved = categoryRepository.save(category);
        return productAndCategoryMapper.mapToCategoryResponse(saved);
    }

    public CategoryResponse getCategoryBySlug(String slug){
        Category category = categoryRepository.findBySlug(slug).orElseThrow(
                () -> new ResourceNotFoundException("Category not found with slug: "+ slug)
        );
        return productAndCategoryMapper.mapToCategoryResponse(category);
    }

    public CategoryResponse getCategoryByName(String name){
        Category category = categoryRepository.findByName(name).orElseThrow(
                () -> new ResourceNotFoundException("Category not found with name: " + name)
        );
        return productAndCategoryMapper.mapToCategoryResponse(category);
    }

    public List<ProductResponse> getAllProductsUnderCategory(UUID categoryId){
        log.debug("Request received to fetch all the products under category id= {}", categoryId);
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                ()-> {
                    log.error("Category not found with id={}", categoryId);
                    throw new ResourceNotFoundException("Category not found");
                }
        );

        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(productAndCategoryMapper::mapToProductResponse)
                .toList();
    }

    public List<CategoryResponse> getSubCategories(UUID categoryId){
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new ResourceNotFoundException("Category not found")
        );

        return category.getSubCategories()
                .stream()
                .map(productAndCategoryMapper::mapToCategoryResponse)
                .toList();
    }

    @Cacheable("parent categories")
    public List<CategoryResponse> getParentCategories(){
        List<Category> parentCategories = categoryRepository.findByParentCategoryIsNull();
        return parentCategories.stream()
                .map(productAndCategoryMapper::mapToCategoryResponse)
                .toList();
    }
}
