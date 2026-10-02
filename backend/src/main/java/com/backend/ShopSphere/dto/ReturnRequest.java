package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ReturnReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnRequest {
    @NotNull(message = "Return reason is required")
    private ReturnReason reason;

    @Size(max = 1000, message = "Provide comment under 1000 characters")
    private String comment;
}
