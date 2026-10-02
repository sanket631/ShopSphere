package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateReturnShipmentRequest {
    @NotNull(message = "Provide the return Id")
    private UUID returnId;

    @NotBlank(message = "Provide carrier")
    private String carrier;
}
