package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CouponCreateRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private DiscountType discountType;

    @NotNull
    @Positive
    private BigDecimal discountValue;

    @PositiveOrZero
    private BigDecimal minimumOrderAmount;

    @Positive
    private BigDecimal maximumDiscount;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime expiryDate;

    @NotNull
    @Positive
    private Integer usageLimit;

    @NotNull
    @Positive
    private Integer usageLimitPerUser;

    private Boolean firstOrderOnly;

    private UUID sellerId;

    private UUID categoryId;
}
