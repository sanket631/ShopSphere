package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.RefundStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundStatusUpdateRequest {
    @NotNull
    private RefundStatus status;

    private String reason;
}
