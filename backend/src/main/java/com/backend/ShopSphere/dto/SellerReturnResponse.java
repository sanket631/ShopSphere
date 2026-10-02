package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ReturnReason;
import com.backend.ShopSphere.enums.ReturnStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SellerReturnResponse {

    private UUID returnId;
    private UUID orderId;
    private UUID orderItemId;

    private UUID productVariantId;
    private String sku;
    private Integer quantity;

    private ReturnStatus status;
    private ReturnReason reason;
    private String customerComment;

    private LocalDateTime requestedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime receivedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime completedAt;

    private String rejectionReason;
}
