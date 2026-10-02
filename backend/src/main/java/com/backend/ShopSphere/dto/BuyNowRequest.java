package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ShippingMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BuyNowRequest extends AddToCartRequest {
    private String couponCode;

    @NotNull(message = "Address is required")
    private UUID addressId;

    @NotNull(message = "Shipping method is required")
    private ShippingMethod shippingMethod;
}