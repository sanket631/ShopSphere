package com.backend.ShopSphere.events;

import com.backend.ShopSphere.enums.EventType;
import com.backend.ShopSphere.enums.ReturnStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ReturnStatusChangedEvent extends BaseEvent {

    private UUID returnId;
    private UUID orderItemId;
    private ReturnStatus status;

    public ReturnStatusChangedEvent(
            UUID orderId,
            UUID returnId,
            UUID orderItemId,
            UUID userId,
            ReturnStatus status
    ) {
        super(
                EventType.RETURN_STATUS_CHANGED,
                orderId,
                userId
        );

        this.returnId = returnId;
        this.orderItemId = orderItemId;
        this.status = status;
    }
}
