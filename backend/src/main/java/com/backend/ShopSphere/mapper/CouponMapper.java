package com.backend.ShopSphere.mapper;

import com.backend.ShopSphere.dto.CouponResponse;
import com.backend.ShopSphere.entity.Coupon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CouponMapper {

    public CouponResponse mapToResponse(Coupon coupon) {
        log.trace("Mapping Coupon entity to CouponResponse - ID: {}, Code: {}",
                coupon.getId(), coupon.getCode());

        CouponResponse response = CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .title(coupon.getTitle())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .startDate(coupon.getStartDate())
                .expiryDate(coupon.getExpiryDate())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .usageLimitPerUser(coupon.getUsageLimitPerUser())
                .status(coupon.getStatus())
                .firstOrderOnly(coupon.getFirstOrderOnly())
                .seller(coupon.getSeller() == null ? null : coupon.getSeller().getFullName())
                .category(coupon.getCategory() == null ? null : coupon.getCategory().getName())
                .build();

        log.trace("Coupon mapping completed - Response: {}", response);
        return response;
    }
}
