package com.backend.ShopSphere.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplyCouponResponse {
    private String couponCode;

    private BigDecimal orderAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private String message;

    private BigDecimal eligibleAmount;

    private Integer eligibleItems;
}
