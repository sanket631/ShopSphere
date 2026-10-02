package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.CancellationReason;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class CancelOrderItemResponse {
    private UUID orderItemId;
    private FulfillmentStatus fulfillmentStatus;
    private CancellationReason cancellationReason;
    private BigDecimal refundAmount;
    private LocalDateTime cancelledAt;
}
