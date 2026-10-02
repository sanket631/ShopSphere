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
public class PaymentSuccessEvent extends BaseEvent{
    private UUID paymentId;

    public PaymentSuccessEvent(UUID orderId, UUID userId, UUID paymentId) {
        super(EventType.PAYMENT_SUCCESS, orderId, userId);
        this.paymentId = paymentId;
    }
}
