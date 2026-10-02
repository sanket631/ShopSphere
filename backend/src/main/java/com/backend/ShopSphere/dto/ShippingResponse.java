package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ShippingMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class ShippingResponse {
    private ShippingMethod shippingMethod;

    private BigDecimal shippingCharge;

    private LocalDate estimatedDelivery;

    private Boolean freeShipping;
}
