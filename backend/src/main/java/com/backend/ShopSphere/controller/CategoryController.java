package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.CategoryRequest;
import com.backend.ShopSphere.dto.CategoryResponse;
import com.backend.ShopSphere.dto.ProductResponse;
import com.backend.ShopSphere.entity.Category;
import com.backend.ShopSphere.entity.Product;
import com.backend.ShopSphere.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CategoryRequest request){
        log.info("=== CREATE CATEGORY API CALLED ===");
        log.debug("Create category request - categoryName: {}", request.getName());

        CategoryResponse createdCategory = categoryService.createCategory(request);

        log.info("Category created successfully - categoryId: {}, categoryName: {}",
                createdCategory.getId(), createdCategory.getName());
        log.debug("Created category details: {}", createdCategory);

        return ResponseEntity.ok(createdCategory);
    }

    @GetMapping
    public List<CategoryResponse> findAll(){
        log.info("=== GET ALL CATEGORIES API CALLED ===");
        log.debug("Fetching all categories");

        List<CategoryResponse> categories = categoryService.getParentCategories();

        int categoryCount = categories.size();

        if(categoryCount == 0) {
            log.info("No categories found in the system");
        } else {
            log.info("Categories retrieved successfully - totalCategories: {}", categoryCount);
        }

        return categories;
    }

    @GetMapping("/all/{id}")
    public List<ProductResponse> getAllProductsUnderCategory(@PathVariable UUID id){
        log.info("=== GET PRODUCTS UNDER CATEGORY API CALLED ===");
        log.debug("Fetching products for categoryId: {}", id);

        List<ProductResponse> products = categoryService.getAllProductsUnderCategory(id);

        int productCount = products.size();

        if(productCount == 0) {
            log.info("No products found under categoryId: {}", id);
        } else {
            log.info("Products retrieved successfully - categoryId: {}, totalProducts: {}", id, productCount);
        }

        return products;
    }
}