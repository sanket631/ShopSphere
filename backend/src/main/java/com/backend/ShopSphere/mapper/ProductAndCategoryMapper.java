package com.backend.ShopSphere.mapper;

import com.backend.ShopSphere.dto.CategoryResponse;
import com.backend.ShopSphere.dto.ProductResponse;
import com.backend.ShopSphere.dto.ProductSearchResponse;
import com.backend.ShopSphere.dto.ProductVariantSummaryResponse;
import com.backend.ShopSphere.entity.Category;
import com.backend.ShopSphere.entity.Product;
import com.backend.ShopSphere.entity.ProductImage;
import com.backend.ShopSphere.entity.ProductVariant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductAndCategoryMapper {

    public CategoryResponse mapToCategoryResponse(Category category){
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentCategoryId(category.getParentCategory() != null ?
                        category.getParentCategory().getId() : null)
                .slug(category.getSlug())
                .parentCategoryName(category.getParentCategory() != null ?
                        category.getParentCategory().getName() : null)
                .path(category.getPath())
                .level(category.getLevel())
                .children(category.getSubCategories() != null ?
                        category.getSubCategories().stream()
                                .map(this::mapToCategoryResponse)
                                .collect(Collectors.toList()) :
                        Collections.emptyList())
                .build();
    }

    public ProductSearchResponse mapToProductSearchResponse(Product product) {
        BigDecimal startingPrice = getMinSellingPrice(product);

        boolean inStock = product.getVariants().stream()
                .anyMatch(variant -> variant.getAvailableStock() != null && variant.getAvailableStock() > 0);

        String primaryImage = product.getVariants().stream()
                .filter(variant -> variant.getProductImages() != null)
                .flatMap(variant -> variant.getProductImages().stream())
                .filter(image -> Boolean.TRUE.equals(image.getPrimaryImage()))
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductSearchResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .brand(product.getBrand())
                .avgRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .shortDescription(product.getShortDescription())
                .startingPrice(startingPrice)
                .primaryImage(primaryImage)
                .totalVariants(product.getVariants() != null ? product.getVariants().size() : 0)
                .inStock(inStock)
                .build();
    }

    private BigDecimal getMinSellingPrice(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return product.getVariants().stream()
                .map(ProductVariant::getSellingPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public ProductResponse mapToProductResponse(Product product) {
        log.trace("Mapping Product entity to ProductResponse DTO for productId={}", product.getId());

        // Map variants to summary responses
        List<ProductVariantSummaryResponse> variantSummaries = product.getVariants() != null ?
                product.getVariants().stream()
                        .map(this::mapToProductVariantSummaryResponse)
                        .collect(Collectors.toList()) :
                List.of();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .brand(product.getBrand())
                .avgRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .shortDescription(product.getShortDescription())
                .longDescription(product.getLongDescription())
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .variants(variantSummaries)
                .build();
    }

    public ProductVariantSummaryResponse mapToProductVariantSummaryResponse(ProductVariant productVariant){
        String primaryImage = productVariant.getProductImages()
                .stream()
                .filter(image ->
                        Boolean.TRUE.equals(image.getPrimaryImage())
                )
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductVariantSummaryResponse.builder()
                .id(productVariant.getId())
                .sku(productVariant.getSku())
                .sellingPrice(productVariant.getSellingPrice())
                .availableStock(productVariant.getAvailableStock())
                .attributes(productVariant.getAttributes())
                .primaryImage(primaryImage)
                .build();
    }
}
