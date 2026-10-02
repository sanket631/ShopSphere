package com.backend.ShopSphere.events;
import com.backend.ShopSphere.enums.CancellationReason;
import com.backend.ShopSphere.enums.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderItemCancelledEvent extends BaseEvent{
    private UUID orderItemId;
    private CancellationReason reason;
    private BigDecimal refundAmount;

    public OrderItemCancelledEvent(UUID orderId, UUID userId, UUID orderItemId,
                                   CancellationReason reason, BigDecimal refundAmount) {
        super(EventType.ORDER_ITEM_CANCELLED, orderId, userId);
        this.orderItemId = orderItemId;
        this.reason = reason;
        this.refundAmount = refundAmount;
    }
}
