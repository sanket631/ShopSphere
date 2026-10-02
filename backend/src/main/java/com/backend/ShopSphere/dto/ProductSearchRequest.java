package com.backend.ShopSphere.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Builder
@Getter
@Setter
public class ProductSearchRequest {
    private String keyword;

    private UUID categoryId;

    private String brand;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Map<String,String> attributes;

    private Boolean inStock;

    private String sortDirection;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    private String sortBy;
}
