package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ShipmentStatus;
import com.backend.ShopSphere.enums.ShipmentType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Builder
@Getter
public class ReturnShipmentResponse {
    private UUID shipmentId;
    private ShipmentType type;

    private UUID returnId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID sellerOrderItemId;

    private UUID productVariantId;
    private String sku;
    private Integer quantity;

    private String trackingNumber;
    private String carrier;

    private ShipmentStatus status;

    private LocalDateTime pickupScheduledAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime receivedAt;
}
