package com.backend.ShopSphere.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {

    private Boolean valid;

    private String message;

    private BigDecimal expectedDiscount;
}
