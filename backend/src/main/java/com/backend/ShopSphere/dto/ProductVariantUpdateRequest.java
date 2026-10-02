package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantUpdateRequest {
    @NotBlank(message = "SKU is required")
    private String sku;

    @NotNull
    private BigDecimal listedPrice;

    @NotNull
    private BigDecimal sellingPrice;

    private BigDecimal weight;

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;

    @NotNull
    private Map<String, String> attributes;

    @NotNull
    @PositiveOrZero
    private Integer availableStock;

    @PositiveOrZero
    private Integer reservedStock;

}
