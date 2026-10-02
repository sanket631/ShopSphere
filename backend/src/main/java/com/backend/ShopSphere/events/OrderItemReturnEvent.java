package com.backend.ShopSphere.events;

import com.backend.ShopSphere.enums.EventType;
import com.backend.ShopSphere.enums.ReturnReason;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderItemReturnEvent extends BaseEvent{
    private UUID returnId;
    private UUID orderItemId;
    private ReturnReason reason;
    private String customerComment;

    public OrderItemReturnEvent(UUID returnId, UUID orderId, UUID userId, UUID orderItemId, ReturnReason reason, String userComment){
        super(EventType.ORDER_ITEM_RETURN, orderId, userId);
        this.returnId=returnId;
        this.orderItemId = orderItemId;
        this.reason = reason;
        this.customerComment = userComment;
    }
}
