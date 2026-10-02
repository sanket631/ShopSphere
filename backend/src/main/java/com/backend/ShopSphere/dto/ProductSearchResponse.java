package com.backend.ShopSphere.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSearchResponse {
    private UUID productId;

    private String name;

    private String slug;

    private String brand;

    private BigDecimal avgRating;

    private Integer reviewCount;

    private String shortDescription;

    private String category;

    private BigDecimal startingPrice;

    private String primaryImage;

    private Integer totalVariants;

    private Boolean inStock;
}
