package com.backend.ShopSphere.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {
    private UUID id;

    private UUID productId;

    private String productName;

    private String sku;

    private BigDecimal listedPrice;

    private BigDecimal sellingPrice;

    private BigDecimal weight;

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;

    private Map<String, String> attributes;

    private Integer availableStock;

    private Integer reservedStock;

    private List<String> imageUrls;
}
