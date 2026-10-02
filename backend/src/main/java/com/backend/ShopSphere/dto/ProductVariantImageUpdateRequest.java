package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductVariantImageUpdateRequest {
    @NotEmpty
    private List<ProductImageRequest> productImages;
}
