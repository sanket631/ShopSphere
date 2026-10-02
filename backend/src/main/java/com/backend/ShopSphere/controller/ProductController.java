package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.entity.Product;
import com.backend.ShopSphere.service.ProductService;
import com.backend.ShopSphere.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    private final ReviewService reviewService;

    @PostMapping
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request){
        log.info("=== CREATE PRODUCT API CALLED ===");

        ProductResponse response = productService.createProduct(request);

        log.info("Product created successfully - productId: {}, productName: {}",
                response.getId(), response.getName());
        log.debug("Created product details: {}", response);

        return response;
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestBody ProductSearchRequest request){
        Page<ProductSearchResponse> responses = productService.search(request);
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "name") String sortBy)
    {
        log.info("=== GET PRODUCTS API CALLED ===");
        log.debug("Fetching products - page: {}, size: {}, sortBy: {}", page, size, sortBy);

        Page<ProductResponse> products = productService.getProducts(page, size, sortBy);

        log.info("Products retrieved successfully - page: {}, totalElements: {}, totalPages: {}, sortBy: {}",
                page, products.getTotalElements(), products.getTotalPages(), sortBy);

        if(products.isEmpty()) {
            log.debug("No products found on page: {}", page);
        }

        return products;
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable UUID id){
        log.info("=== GET PRODUCT BY ID API CALLED ===");
        log.debug("Fetching product details - productId: {}", id);

        ProductResponse response = productService.getProductById(id);

        log.info("Product retrieved successfully - productId: {}, productName: {}",
                response.getId(), response.getName());
        log.debug("Product details: {}", response);

        return response;
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable UUID id){
        log.info("=== DELETE PRODUCT API CALLED ===");
        log.warn("Product deletion requested - productId: {}", id);

        productService.deleteProduct(id);

        log.info("Product deleted successfully - productId: {}", id);
    }

    @PutMapping("/update/{id}")
    public ProductResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request){
        log.info("=== UPDATE PRODUCT API CALLED ===");
        log.debug("Update product request - productId: {}, name: {}",
                id, request.getName());

        ProductResponse response = productService.updateProduct(id, request);

        log.info("Product updated successfully - productId: {}, productName: {}",
                response.getId(), response.getName());
        log.debug("Updated product details: {}", response);

        return response;
    }

    @GetMapping("/category/{id}")
    public List<ProductResponse> findByCategory(@PathVariable UUID id){
        log.info("=== FIND PRODUCTS BY CATEGORY API CALLED ===");
        log.debug("Fetching products for category: {}", id);

        List<ProductResponse> products = productService.getProductsByCategory(id);

        int productCount = products.size();

        if(productCount == 0) {
            log.info("No products found in category: {}", id);
        } else {
            log.info("Products retrieved for category '{}' - totalProducts: {}", id, productCount);
            log.debug("Products in category {}: {}", id, products.stream()
                    .map(p -> p.getName())
                    .collect(Collectors.toList()));
        }

        return products;
    }

    @GetMapping("/review/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        return ResponseEntity.ok(
                reviewService.getReviews(productId,page,size)
        );
    }

    @GetMapping("/review/{productId}/summary")
    public ResponseEntity<RatingSummaryResponse> getRatingSummary(
            @PathVariable UUID productId){

        return ResponseEntity.ok(
                reviewService.getRatingSummary(productId)
        );
    }

    //upload files
    @PostMapping(value = "/images/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductImageResponse uploadProductImage(
            @PathVariable UUID productId,
            @RequestParam ("file")MultipartFile file) {
        return productService.uploadProductImage(productId, file);
    }
}