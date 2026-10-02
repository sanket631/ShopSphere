package com.backend.ShopSphere.enums;

public enum ShipmentStatus {
    CREATED,
    SHIPPED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    DELIVERY_FAILED,

    // Return shipment
    PICKUP_SCHEDULED,
    PICKED_UP,
    RECEIVED
}
