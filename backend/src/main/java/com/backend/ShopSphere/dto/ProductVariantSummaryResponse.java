package com.backend.ShopSphere.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariantSummaryResponse {
    private UUID id;

    private String sku;

    private BigDecimal sellingPrice;

    private Integer availableStock;

    private Map<String, String> attributes;

    private String primaryImage;
}
