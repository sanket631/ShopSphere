package com.backend.ShopSphere.events;

import com.backend.ShopSphere.enums.EventType;
import com.backend.ShopSphere.enums.RefundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RefundStatusChangedEvent extends BaseEvent{
    private UUID refundId;
    private UUID orderItemId;
    private RefundStatus status;
    private BigDecimal amount;

    public RefundStatusChangedEvent(
            UUID orderId,
            UUID refundId,
            UUID orderItemId,
            UUID userId,
            RefundStatus status,
            BigDecimal amount
    ) {
        super(
                EventType.REFUND_STATUS_CHANGED,
                orderId,
                userId
        );

        this.refundId = refundId;
        this.orderItemId = orderItemId;
        this.status = status;
        this.amount = amount;
    }
}
