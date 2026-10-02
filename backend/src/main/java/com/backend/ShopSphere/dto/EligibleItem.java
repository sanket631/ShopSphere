package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.entity.ProductVariant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class EligibleItem {
    private ProductVariant productVariant;

    private Integer quantity;

    private BigDecimal amount;
}
