package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ShippingMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {
    private String couponCode;

    @NotNull
    private UUID addressId;

    @NotNull(message = "Shipping method is required")
    private ShippingMethod shippingMethod;
}
