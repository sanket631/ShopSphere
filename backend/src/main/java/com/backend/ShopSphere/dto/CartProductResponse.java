package com.backend.ShopSphere.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartProductResponse {
    private UUID cartItemId;
    private UUID productId;
    private UUID variantId;
    private String name;
    private String sku;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalPrice;
}
