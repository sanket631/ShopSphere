package com.backend.ShopSphere.events;
import com.backend.ShopSphere.enums.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PaymentFailedEvent extends BaseEvent{
    private UUID paymentId;
    private String failureReason;

    public PaymentFailedEvent(UUID orderId, UUID userId, UUID paymentId, String failureReason) {
        super(EventType.PAYMENT_FAILURE, orderId, userId);
        this.paymentId = paymentId;
        this.failureReason = failureReason;
    }
}
