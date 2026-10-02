package com.backend.ShopSphere.events;

import com.backend.ShopSphere.enums.EventType;
import com.backend.ShopSphere.enums.ShipmentStatus;
import com.backend.ShopSphere.enums.ShipmentType;
import lombok.*;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ShipmentStatusChangedEvent extends BaseEvent{
    private ShipmentType shipmentType;
    private UUID shipmentId;
    private ShipmentStatus previousStatus;
    private ShipmentStatus newStatus;

    public ShipmentStatusChangedEvent(UUID orderId, UUID userId, UUID shipmentId,
                                      ShipmentStatus previousStatus, ShipmentStatus newStatus, ShipmentType shipmentType) {
        super(EventType.SHIPMENT_STATUS_CHANGED, orderId, userId);
        this.shipmentId = shipmentId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.shipmentType = shipmentType;
    }
}
