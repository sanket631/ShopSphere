package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.ProductVariantSummaryResponse;
import com.backend.ShopSphere.entity.ProductImage;
import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.entity.WishListItem;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.ProductVariantRepository;
import com.backend.ShopSphere.repository.WishListItemRepository;
import com.backend.ShopSphere.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishListItemService {
    private final WishListItemRepository wishListItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    @CacheEvict(value = "wishlist", key = "#root.target.getCacheKey()")
    public void addWishListItem(UUID variantId) {
        log.debug("Add to wishlist request - variantId: {}", variantId);

        // Get user ID
        UUID userId = securityUtil.getCurrentUserId();
        log.debug("Current user ID: {}", userId);

        // Get product variant
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> {
                    log.error("Variant not found with id: {}", variantId);
                    return new ResourceNotFoundException("Variant not found");
                });
        log.debug("Variant found - id: {}, sku: {}", variant.getId(), variant.getSku());

        // Check if already in wishlist
        if (wishListItemRepository.existsByUserIdAndProductVariantId(userId, variantId)) {
            log.warn("Variant {} already exists in wishlist for user: {}", variantId, userId);
            throw new IllegalArgsException("Product already exists in wishlist");
        }

        // Create user reference
        User userReference = User.builder()
                .id(userId)
                .build();

        // Create wishlist item
        WishListItem item = WishListItem.builder()
                .user(userReference)
                .productVariant(variant)
                .build();

        wishListItemRepository.save(item);
        log.info("Wishlist item created successfully - variantId={}, userId={}", variantId, userId);
    }

    @Cacheable(value = "wishlist", key = "#root.target.getCacheKey()")
    public List<ProductVariantSummaryResponse> getWishList() {
        UUID userId = securityUtil.getCurrentUserId();
        log.info("Fetching wishlist for user={}", userId);
        List<WishListItem> wishlistItems = wishListItemRepository.findByUserId(userId);
        log.info("Retrieved {} wishlist items for user", wishlistItems.size());

        if (wishlistItems.isEmpty()) {
            log.warn("Wishlist is empty for user: {}", userId);
        }

        return wishlistItems.stream()
                .map(item -> mapToProductVariantSummaryResponse(item.getProductVariant()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "wishlist", key = "#root.target.getCacheKey()")
    public void removeWishListItem(UUID variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        UUID userId = securityUtil.getCurrentUserId();

        WishListItem item = wishListItemRepository
                .findByUserIdAndProductVariantId(userId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));

        wishListItemRepository.delete(item);
        log.info("Wishlist item removed successfully. variantId={}", variantId);
    }

    public boolean isWishListed(UUID variantId) {
        log.debug("Checking if variant {} is in wishlist", variantId);
        
        UUID userId = securityUtil.getCurrentUserId();
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        
        return wishListItemRepository.existsByUserIdAndProductVariantId(userId, variantId);
    }

    public String getCacheKey() {
        return "wishlist_" + securityUtil.getCurrentUserId().toString();
    }

    private ProductVariantSummaryResponse mapToProductVariantSummaryResponse(ProductVariant variant) {
        String primaryImageUrl = variant.getProductImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getPrimaryImage()))
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductVariantSummaryResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .sellingPrice(variant.getSellingPrice())
                .availableStock(variant.getAvailableStock())
                .attributes(variant.getAttributes())
                .primaryImage(primaryImageUrl)
                .build();
    }
}