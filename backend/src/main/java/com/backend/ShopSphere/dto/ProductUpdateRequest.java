package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductUpdateRequest {
    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    private String slug;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Short description is required")
    private String shortDescription;

    @NotBlank(message = "Long description is required")
    private String longDescription;

    @NotNull(message = "Category is required")
    private UUID categoryId;
}
