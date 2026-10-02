package com.backend.ShopSphere.events;
import com.backend.ShopSphere.enums.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ReservationExpiredEvent extends BaseEvent{
    public ReservationExpiredEvent(UUID orderId, UUID userId) {
        super(EventType.RESERVATION_EXPIRED, orderId, userId);
    }
}
