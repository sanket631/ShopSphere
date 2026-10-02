package com.backend.ShopSphere.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private UUID id;

    private String name;

    private String slug;

    private String brand;

    private BigDecimal avgRating = BigDecimal.ZERO;

    private Integer reviewCount = 0;

    private String shortDescription;

    private String longDescription;

    private String category;

    private boolean isReturnApplicable;

    private Integer returnWindow;

    private List<ProductVariantSummaryResponse> variants;
}
