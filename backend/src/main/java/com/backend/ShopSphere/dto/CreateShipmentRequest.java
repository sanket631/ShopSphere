package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateShipmentRequest {
    @NotNull
    private UUID sellerOrderItemId;

    @NotBlank
    private String carrier;

}
