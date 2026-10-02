package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ReturnReason;
import com.backend.ShopSphere.enums.ReturnStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ReturnResponse {
    private UUID returnId;
    private UUID orderId;
    private UUID orderItemId;

    private ReturnStatus status;
    private ReturnReason reason;

    private String customerComment;

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;

    private String rejectionReason;
}
