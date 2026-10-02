package com.backend.ShopSphere.enums;

public final class EventConsumers {

    private EventConsumers() {}

    // Inventory
    public static final String INVENTORY_EVENT_PROCESSOR = "INVENTORY_EVENT_PROCESSOR";

    // Cart
    public static final String CART_CLEANUP = "CART_CLEANUP";

    // Notification
    public static final String NOTIFICATION_PROCESSOR = "NOTIFICATION_PROCESSOR";

    // Coupon
    public static final String COUPON_CONSUMPTION = "COUPON_CONSUMPTION";

    // Seller
    public static final String SELLER_FULFILLMENT = "SELLER_FULFILLMENT";

    // Shipment
    public static final String SHIPMENT_LIFECYCLE = "SHIPMENT_LIFECYCLE";

    // Refund
    public static final String REFUND_PROCESSOR = "REFUND_PROCESSOR";

    // Return
    public static final String RETURN_PROCESSOR = "RETURN_PROCESSOR";
}