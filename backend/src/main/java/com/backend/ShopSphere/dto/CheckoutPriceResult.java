package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.entity.Coupon;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CheckoutPriceResult {
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingCharge;
    private BigDecimal finalAmount;
    private Coupon coupon;
    private ShippingResponse shipping;
}
