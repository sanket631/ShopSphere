package com.backend.ShopSphere.listener;

import com.backend.ShopSphere.enums.EventType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventListenerRegistry {

    private final Map<EventType, List<EventListener>> listeners = new ConcurrentHashMap<>();

    // All listeners
    private final CartCleanupListener cartCleanupListener;
    private final InventoryEventListener inventoryListener;
    private final NotificationListener notificationListener;
    private final CouponListener couponListener;
    private final SellerFulfillmentListener sellerFulfillmentListener;
    private final ShipmentEventListener shipmentEventListener;
    private final RefundListener refundListener;
    private final ReturnEventListener returnEventListener;

    @PostConstruct
    public void init() {
        // PAYMENT_SUCCESS
        register(EventType.PAYMENT_SUCCESS, cartCleanupListener);
        register(EventType.PAYMENT_SUCCESS, inventoryListener);
        register(EventType.PAYMENT_SUCCESS, notificationListener);
        register(EventType.PAYMENT_SUCCESS, couponListener);
        register(EventType.PAYMENT_SUCCESS, sellerFulfillmentListener);

        // PAYMENT_FAILURE
        register(EventType.PAYMENT_FAILURE, inventoryListener);
        register(EventType.PAYMENT_FAILURE, notificationListener);

        // RESERVATION_EXPIRED
        register(EventType.RESERVATION_EXPIRED, inventoryListener);
        register(EventType.RESERVATION_EXPIRED, notificationListener);

        // ORDER_ITEM_CANCELLED
        register(EventType.ORDER_ITEM_CANCELLED, inventoryListener);
        register(EventType.ORDER_ITEM_CANCELLED, notificationListener);
        register(EventType.ORDER_ITEM_CANCELLED, refundListener);

        // SHIPMENT_STATUS_CHANGED
        register(EventType.SHIPMENT_STATUS_CHANGED, shipmentEventListener);

        // REFUND_STATUS_CHANGED
        register(EventType.REFUND_STATUS_CHANGED, refundListener);
        register(EventType.REFUND_STATUS_CHANGED, returnEventListener);

        // ORDER_ITEM_RETURN
        register(EventType.ORDER_ITEM_RETURN, returnEventListener);
        register(EventType.ORDER_ITEM_RETURN, notificationListener);

        // RETURN_STATUS_CHANGED
        register(EventType.RETURN_STATUS_CHANGED, returnEventListener);
        register(EventType.RETURN_STATUS_CHANGED, notificationListener);
        register(EventType.RETURN_STATUS_CHANGED, inventoryListener);

        log.info("Event listeners registered: {}", listeners.keySet());
    }

    private void register(EventType eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
        log.debug("Registered listener {} for event {}",
                listener.getClass().getSimpleName(), eventType);
    }

    public List<EventListener> getListeners(EventType eventType) {
        return listeners.getOrDefault(eventType, Collections.emptyList());
    }
}