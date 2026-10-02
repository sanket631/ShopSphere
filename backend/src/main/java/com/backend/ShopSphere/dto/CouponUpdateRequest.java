package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.CouponStatus;
import com.backend.ShopSphere.enums.DiscountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponUpdateRequest {

    private String title;

    private String description;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal minimumOrderAmount;

    private BigDecimal maximumDiscount;

    private LocalDateTime startDate;

    private LocalDateTime expiryDate;

    private Integer usageLimit;

    private Integer usageLimitPerUser;

    private CouponStatus status;

    private Boolean firstOrderOnly;

}