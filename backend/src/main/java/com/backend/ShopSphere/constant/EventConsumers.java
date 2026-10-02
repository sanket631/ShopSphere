package com.backend.ShopSphere.constant;

public final class EventConsumers {

    public static final String CART_CLEANUP = "CART_CLEANUP";
    public static final String COUPON_CONSUMPTION = "COUPON_CONSUMPTION";
    public static final String INVENTORY_CONFIRMATION = "INVENTORY_CONFIRMATION";
    public static final String INVENTORY_RELEASE = "INVENTORY_RELEASE";
    public static final String INVENTORY_RESTORE = "INVENTORY_RESTORE";
    public static final String SELLER_FULFILLMENT = "SELLER_FULFILLMENT";
    public static final String CUSTOMER_NOTIFICATION = "CUSTOMER_NOTIFICATION";
    public static final String SELLER_NOTIFICATION = "SELLER_NOTIFICATION";
    public static final String INVENTORY_CANCELLATION = "INVENTORY_CANCELLATION";
    public static final String SHIPMENT_LIFECYCLE = "SHIPMENT_LIFECYCLE";

    private EventConsumers() {}
}
