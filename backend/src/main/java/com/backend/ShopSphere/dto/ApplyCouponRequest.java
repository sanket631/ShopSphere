package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.entity.Cart;
import com.backend.ShopSphere.entity.ProductVariant;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyCouponRequest {

    @NotBlank
    private String couponCode;

}