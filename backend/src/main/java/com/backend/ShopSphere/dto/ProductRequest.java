package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {
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

    @NotNull(message = "Provide return policy applicable or not. Provide true or false")
    private Boolean isReturnApplicable;

    @Positive
    @Min(value = 3, message = "Return window must be greater than 2 days")
    private Integer returnWindowDays;

    @NotEmpty(message = "Provide at least one product variant")
    private List<ProductVariantRequest> variants;

    @AssertTrue(message = "Return window days is required when return is applicable")
    public boolean isReturnWindowValid() {
        if (Boolean.TRUE.equals(isReturnApplicable)) {
            return returnWindowDays != null && returnWindowDays >= 3;
        }
        return true;
    }
}
