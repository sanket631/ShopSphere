package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.CouponStatus;
import com.backend.ShopSphere.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {

    private UUID id;

    private String code;

    private String title;

    private String description;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal minimumOrderAmount;

    private BigDecimal maximumDiscount;

    private LocalDateTime startDate;

    private LocalDateTime expiryDate;

    private Integer usageLimit;

    private Integer usedCount;

    private Integer usageLimitPerUser;

    private CouponStatus status;

    private Boolean firstOrderOnly;

    private String seller;

    private String category;
}