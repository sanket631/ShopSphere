package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.RefundStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class RefundResponse {
    private UUID refundId;
    private UUID orderId;
    private UUID orderItemId;

    private BigDecimal amount;
    private RefundStatus status;

    private String refundTransactionId;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime processedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime rejectedAt;

    private UUID productVariantId;
    private String sku;
    private Integer quantity;
}
