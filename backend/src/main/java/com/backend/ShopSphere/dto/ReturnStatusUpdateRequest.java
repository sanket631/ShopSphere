package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ReturnStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnStatusUpdateRequest {

    @NotNull(message = "Return status is required")
    private ReturnStatus status;

    private String reason;

    private Boolean approveRefundImmediately;
}
