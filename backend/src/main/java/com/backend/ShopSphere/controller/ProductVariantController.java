package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.ProductVariantImageUpdateRequest;
import com.backend.ShopSphere.dto.ProductVariantRequest;
import com.backend.ShopSphere.dto.ProductVariantUpdateRequest;
import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-variants")
public class ProductVariantController {
    private final ProductVariantService productVariantService;

    @PostMapping
    public ResponseEntity<?> addVariant(@Valid @RequestBody ProductVariantRequest request){
        return ResponseEntity.ok(productVariantService.addVariant(request));
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<?> updateVariant(@Valid @RequestBody ProductVariantUpdateRequest request, @PathVariable UUID variantId){
        return ResponseEntity.ok(productVariantService.updateVariant(variantId, request));
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<?> getVariant(@PathVariable UUID variantId){
        return ResponseEntity.ok(productVariantService.getProductVariant(variantId));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<?> deleteVariant(@PathVariable UUID variantId) {
        productVariantService.deleteVariant(variantId);
        return ResponseEntity.ok("Product Variant deleted successfully");
    }

    @DeleteMapping("/image/{imageId}")
    public ResponseEntity<?> deleteVariantImage(@PathVariable UUID imageId){
        productVariantService.deleteVariantImage(imageId);
        return ResponseEntity.ok("Product image deleted successfully");
    }

    @PutMapping("/image/{variantId}")
    public ResponseEntity<?> updateVariantImage(@PathVariable UUID variantId, @RequestBody ProductVariantImageUpdateRequest request){
        return ResponseEntity.ok(productVariantService.updateVariantImages(variantId, request));
    }
}
