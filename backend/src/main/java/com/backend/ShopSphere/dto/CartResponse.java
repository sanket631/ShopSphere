package com.backend.ShopSphere.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartResponse {
    private UUID cartId;
    private BigDecimal totalAmount;
    private Integer totalItems;
    List<CartProductResponse> products;
}
