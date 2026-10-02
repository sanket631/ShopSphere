package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.entity.Product;
import com.backend.ShopSphere.entity.ProductImage;
import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.ProductImageRepository;
import com.backend.ShopSphere.repository.ProductRepository;
import com.backend.ShopSphere.repository.ProductVariantRepository;
import com.backend.ShopSphere.security.SecurityUtil;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductVariantService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final SecurityUtil securityUtil;
    private final ProductImageRepository productImageRepository;

    @Transactional
    @CacheEvict(
            value = {
                    CacheConstants.PRODUCTS,
                    CacheConstants.PRODUCT_VARIANTS
            },
            allEntries = true
    )
    public ProductVariantSummaryResponse addVariant(ProductVariantRequest request){
        log.info(
                "Adding new variant. productId={}, sku={}",
                request.getProductId(),
                request.getSku()
        );

        UUID productId = request.getProductId();
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException("Product not found")
        );

        securityUtil.validateProductOwnership(product.getSeller().getEmail());

        if(productVariantRepository.existsBySku(request.getSku())){
            throw new IllegalArgsException("SKU already exists: " + request.getSku());
        }

        ProductVariant productVariant = ProductVariant.builder()
                .product(product)
                .sku(request.getSku())
                .attributes(request.getAttributes())
                .availableStock(request.getAvailableStock())
                .reservedStock(request.getReservedStock())
                .listedPrice(request.getListedPrice())
                .sellingPrice(request.getSellingPrice())
                .height(request.getHeight())
                .width(request.getWidth())
                .length(request.getLength())
                .weight(request.getWeight())
                .build();
        List<ProductImage> images = addImagesToVariants(request.getProductImages(), productVariant);
        productVariant.setProductImages(images);
        log.debug("{} images added for sku={}", images.size(), request.getSku());

        ProductVariant saved = productVariantRepository.save(productVariant);
        log.info(
                "Variant created successfully. variantId={}, sku={}",
                saved.getId(),
                saved.getSku()
        );
        return mapToProductVariantSummaryResponse(saved);
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

    private List<ProductImage> addImagesToVariants(@NotEmpty List<ProductImageRequest> productImageRequests, ProductVariant productVariant) {
        List<ProductImage> images = new ArrayList<>();

        validatePrimaryImageAndImageOrder(productImageRequests);
        return productImageRequests.stream()
                .sorted(
                        Comparator.comparing(
                                ProductImageRequest::getDisplayOrder
                        )
                )
                .map(image ->
                    ProductImage.builder()
                            .imageUrl(image.getImageUrl())
                            .displayOrder(image.getDisplayOrder())
                            .primaryImage(image.getPrimaryImage())
                            .productVariant(productVariant)
                            .build()
                )
                .collect(Collectors.toList());

    }

    private void validatePrimaryImageAndImageOrder(@NotEmpty List<ProductImageRequest> productImageRequests) {
        int count = 0;
        Set<Integer> orders = new HashSet<>();

        for(ProductImageRequest image : productImageRequests){

            if(!orders.add(image.getDisplayOrder())){
                throw new IllegalArgsException(
                        "Duplicate display order. Provide distinct image order"
                );
            }

            if(Boolean.TRUE.equals(image.getPrimaryImage())){
                count++;
            }
        }

        if(count > 1){
            throw new IllegalArgsException("Exactly one primary image is required for the variant");
        } else if(count == 0) {
            throw new IllegalArgsException("Select one primary image for the variant");
        }
    }

    public ProductVariantSummaryResponse getProductVariant(UUID variantId){
        ProductVariant variant = productVariantRepository.findById(variantId).orElseThrow(
                () -> new ResourceNotFoundException("Product Variant not found")
        );

        return mapToProductVariantSummaryResponse(variant);
    }

    @Transactional
    @CacheEvict(
            value = {
                    CacheConstants.PRODUCTS,
                    CacheConstants.PRODUCT_VARIANTS
            },
            allEntries = true
    )
    public ProductVariantSummaryResponse updateVariant(
            UUID variantId,
            ProductVariantUpdateRequest request
    ){
        ProductVariant variant = findVariant(variantId);

        securityUtil.validateProductOwnership(variant.getProduct().getSeller().getEmail());

        if(!variant.getSku().equals(request.getSku())
                        &&
                        productVariantRepository.existsBySku(
                                request.getSku()
                        )
        ){
            throw new IllegalArgsException("SKU already exists");
        }

        variant.setSku(request.getSku());

        variant.setListedPrice(request.getListedPrice());

        variant.setSellingPrice(request.getSellingPrice());

        variant.setWeight(request.getWeight());

        variant.setLength(request.getLength());

        variant.setWidth(request.getWidth());

        variant.setHeight(request.getHeight());

        variant.setAvailableStock(
                request.getAvailableStock()
        );

        variant.setReservedStock(
                request.getReservedStock()
        );

        variant.setAttributes(
                request.getAttributes()
        );

        ProductVariant saved =
                productVariantRepository.save(
                        variant
                );

        log.info(
                "Variant updated successfully. variantId={}",
                saved.getId()
        );

        return mapToProductVariantSummaryResponse(saved);
    }

    @Transactional
    @CacheEvict(
            value = {
                    CacheConstants.PRODUCTS,
                    CacheConstants.PRODUCT_VARIANTS
            },
            allEntries = true
    )
    public ProductVariantSummaryResponse updateVariantImages(
            UUID variantId,
            ProductVariantImageUpdateRequest request
    ){
        ProductVariant variant = findVariant(variantId);

        securityUtil.validateProductOwnership(
                variant.getProduct()
                        .getSeller()
                        .getEmail()
        );

        validatePrimaryImageAndImageOrder(request.getProductImages());

        variant.getProductImages().clear();

        List<ProductImage> images =
                addImagesToVariants(
                        request.getProductImages(),
                        variant
                );

        variant.getProductImages().addAll(images);

        ProductVariant saved =
                productVariantRepository.save(
                        variant
                );

        log.info(
                "Variant images updated. variantId={}, totalImages={}",
                saved.getId(),
                saved.getProductImages().size()
        );

        return mapToProductVariantSummaryResponse(saved);
    }

    @Transactional
    @CacheEvict(
            value = {
                    CacheConstants.PRODUCTS,
                    CacheConstants.PRODUCT_VARIANTS
            },
            allEntries = true
    )
    public void deleteVariantImage(UUID imageId){
        ProductImage image = productImageRepository.findById(imageId).orElseThrow(
                () -> new ResourceNotFoundException("Image not found")
        );

        ProductVariant variant = image.getProductVariant();

        securityUtil.validateProductOwnership(
                variant.getProduct()
                        .getSeller()
                        .getEmail()
        );

        if(variant.getProductImages().size()==1){
            throw new IllegalArgsException("Variant must contain at least one image");
        }

        boolean primary = Boolean.TRUE.equals(image.getPrimaryImage());

        variant.getProductImages().remove(image);

        if(primary){
            variant.getProductImages()
                    .stream()
                    .min(
                            Comparator.comparing(
                                    ProductImage::getDisplayOrder
                            )
                    )
                    .ifPresent(
                            img -> img.setPrimaryImage(true)
                    );
        }

        normalizeDisplayOrder(variant.getProductImages());

        productVariantRepository.save(variant);

        log.info("Variant image deleted. imageId={}", imageId);
    }

    private void normalizeDisplayOrder(
            List<ProductImage> images
    ){

        images.sort(
                Comparator.comparing(
                        ProductImage::getDisplayOrder
                )
        );

        for(int i=0;i<images.size();i++){

            images.get(i)
                    .setDisplayOrder(i+1);
        }
    }

    @CacheEvict(
            value = {
                    CacheConstants.PRODUCTS,
                    CacheConstants.PRODUCT_VARIANTS
            },
            allEntries = true
    )
    public void deleteVariant(UUID variantId){
        ProductVariant variant = findVariant(variantId);
        Product product = variant.getProduct();
        securityUtil.validateProductOwnership(product.getSeller().getEmail());

        if(product.getVariants().size()==1){
            throw new IllegalArgsException(
                    "Product must have at least one variant."
            );
        }
        productVariantRepository.delete(variant);
        log.info(
                "Variant deleted successfully. variantId={}",
                variantId
        );
    }

    private ProductVariant findVariant(UUID variantId){
        return productVariantRepository.findById(variantId).orElseThrow(
                () -> new ResourceNotFoundException("Product variant not found")
        );
    }
}
