package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.CancellationReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderItemRequest {
    @NotNull(message = "Cancellation reason is required")
    private CancellationReason reason;
}
