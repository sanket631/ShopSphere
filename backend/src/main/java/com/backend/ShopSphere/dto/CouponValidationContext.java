package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.entity.Cart;
import com.backend.ShopSphere.entity.Coupon;
import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.entity.User;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CouponValidationContext {
    private Coupon coupon;

    private User user;

    private Cart cart;

    private ProductVariant buyNowVariant;

    private Integer buyNowQuantity;

    private BigDecimal totalOrderAmount;
}
