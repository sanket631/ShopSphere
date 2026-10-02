package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShipmentStatusRequest {
    @NotNull
    private ShipmentStatus status;
}
