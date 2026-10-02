package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.entity.ProductImage;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {
    @NotNull(message = "Product is required")
    private UUID productId;

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotNull(message = "Listed price is required")
    @DecimalMin(value = "0.0")
    private BigDecimal listedPrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0")
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.0")
    private BigDecimal weight;

    @DecimalMin(value = "0.0")
    private BigDecimal length;

    @DecimalMin(value = "0.0")
    private BigDecimal width;

    @DecimalMin(value = "0.0")
    private BigDecimal height;

    @NotNull(message = "Attributes are required")
    private Map<String, String> attributes;

    @NotNull(message = "Available stock is required")
    @PositiveOrZero
    private Integer availableStock;

    @PositiveOrZero
    private Integer reservedStock;

    @NotNull
    private List<ProductImageRequest> productImages;

}
