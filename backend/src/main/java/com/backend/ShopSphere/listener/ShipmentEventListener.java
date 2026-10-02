package com.backend.ShopSphere.listener;

import com.backend.ShopSphere.constant.EntityType;
import com.backend.ShopSphere.dto.AuditRequest;
import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.entity.Return;
import com.backend.ShopSphere.enums.*;
import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.entity.SellerOrderItem;
import com.backend.ShopSphere.entity.Shipment;
import com.backend.ShopSphere.events.ReturnStatusChangedEvent;
import com.backend.ShopSphere.events.ShipmentStatusChangedEvent;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.ReturnRepository;
import com.backend.ShopSphere.repository.ShipmentRepository;
import com.backend.ShopSphere.service.AuditService;
import com.backend.ShopSphere.service.EventPublisher;
import com.backend.ShopSphere.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShipmentEventListener implements EventListener {

    private final ShipmentRepository shipmentRepository;
    private final NotificationService notificationService;
    private final ReturnRepository returnRepository;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;

    @Transactional
    @Override
    public void process(BaseEvent event) {
        // Only handle SHIPMENT_STATUS_CHANGED events
        if (event.getEventType() != EventType.SHIPMENT_STATUS_CHANGED) {
            return;
        }

        ShipmentStatusChangedEvent shipmentEvent = (ShipmentStatusChangedEvent) event;
        log.info("Processing shipment status change: shipmentId={}", shipmentEvent.getShipmentId());

        Shipment shipment = shipmentRepository.findById(shipmentEvent.getShipmentId())
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (ShipmentType.FORWARD.equals(shipmentEvent.getShipmentType())) {
            updateFulfillmentStatus(shipment, shipmentEvent.getNewStatus());
        } else if (ShipmentType.RETURN.equals(shipmentEvent.getShipmentType())) {
            updateReturnStatus(shipment, shipmentEvent.getNewStatus());
        }

        // Send customer notification
        sendCustomerNotification(shipment, shipmentEvent.getNewStatus());

        log.info("Shipment status change processed: shipmentId={}, newStatus={}",
                shipment.getId(), shipmentEvent.getNewStatus());
    }

    private void updateReturnStatus(Shipment shipment, ShipmentStatus shipmentStatus) {
        OrderItem orderItem = shipment.getSellerOrderItem().getOrderItem();

        Return returnEntity = returnRepository
                .findByOrderItemId(orderItem.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Return not found for order item: " + orderItem.getId()
                        ));

        ReturnStatus nextStatus = switch (shipmentStatus) {
            case PICKED_UP -> ReturnStatus.PICKED_UP;
            case RECEIVED -> ReturnStatus.RECEIVED;
            default -> null;
        };

        if (nextStatus == null) {
            return;
        }

        ReturnStatus currentStatus = returnEntity.getStatus();

        if (currentStatus.equals(nextStatus)) {
            log.debug(
                    "Return already has status. returnId={}, status={}",
                    returnEntity.getId(),
                    currentStatus
            );
            return;
        }

        returnEntity.setStatus(nextStatus);

        LocalDateTime now = LocalDateTime.now();

        if (ReturnStatus.PICKED_UP.equals(nextStatus)) {
            returnEntity.setPickedUpAt(now);
        }

        if (ReturnStatus.RECEIVED.equals(nextStatus)) {
            returnEntity.setReceivedAt(now);
        }

        Return savedReturn = returnRepository.save(returnEntity);

        log.info(
                "Return status updated from shipment. returnId={}, orderItemId={}, " +
                        "shipmentId={}, from={}, to={}",
                savedReturn.getId(),
                orderItem.getId(),
                shipment.getId(),
                currentStatus,
                nextStatus
        );

        AuditRequest auditRequest = new AuditRequest(
                EntityType.RETURN,
                returnEntity.getId(),
                currentStatus.name(),
                nextStatus.name(),
                "RETURN_SHIPMENT_STATUS_CHANGED",
                null,
                "SYSTEM",
                "Return status updated from return shipment"
        );

        auditService.recordStatusChange(auditRequest);
        publishReturnStatusChangedAfterCommit(savedReturn);
    }

    private void updateFulfillmentStatus(Shipment shipment, ShipmentStatus shipmentStatus) {
        SellerOrderItem sellerOrderItem = shipment.getSellerOrderItem();
        FulfillmentStatus fulfillmentStatus = mapToFulfillmentStatus(shipmentStatus);

        if (fulfillmentStatus == null || fulfillmentStatus.equals(sellerOrderItem.getStatus())) {
            return;
        }

        FulfillmentStatus oldSellerStatus = sellerOrderItem.getStatus();
        FulfillmentStatus oldOrderItemStatus = sellerOrderItem.getOrderItem().getStatus();

        sellerOrderItem.setStatus(fulfillmentStatus);
        sellerOrderItem.getOrderItem().setStatus(fulfillmentStatus);
        
        if(ShipmentStatus.DELIVERED.equals(shipmentStatus)){
            sellerOrderItem.getOrderItem().setDeliveredAt(LocalDateTime.now());
            sellerOrderItem.getOrderItem().setDeliveredAt(LocalDateTime.now());
            log.info("Order Item with id: {} delivered at {}", sellerOrderItem.getOrderItem().getId(), LocalDateTime.now());
        }

        AuditRequest sellerAudit = new AuditRequest(
                EntityType.SELLER_ORDER_ITEM,
                sellerOrderItem.getId(),
                oldSellerStatus.name(),
                fulfillmentStatus.name(),
                "SHIPMENT_STATUS_CHANGED",
                null,
                "SYSTEM",
                "Seller order item status updated from shipment"
        );

        auditService.recordStatusChange(sellerAudit);

        AuditRequest orderItemAudit = new AuditRequest(
                EntityType.ORDER_ITEM,
                sellerOrderItem.getOrderItem().getId(),
                oldOrderItemStatus.name(),
                fulfillmentStatus.name(),
                "SHIPMENT_STATUS_CHANGED",
                null,
                "SYSTEM",
                "Order item status updated from shipment"
        );

        auditService.recordStatusChange(orderItemAudit);

        log.debug("Fulfillment status updated: sellerOrderItemId={}, status={}",
                sellerOrderItem.getId(), fulfillmentStatus);
    }

    private FulfillmentStatus mapToFulfillmentStatus(ShipmentStatus shipmentStatus) {
        return switch (shipmentStatus) {
            case SHIPPED -> FulfillmentStatus.SHIPPED;
            case IN_TRANSIT -> FulfillmentStatus.IN_TRANSIT;
            case OUT_FOR_DELIVERY -> FulfillmentStatus.OUT_FOR_DELIVERY;
            case DELIVERED -> FulfillmentStatus.DELIVERED;
            case CANCELLED -> FulfillmentStatus.CANCELLED;
            case CREATED, DELIVERY_FAILED, PICKUP_SCHEDULED, PICKED_UP, RECEIVED -> null;
        };
    }

    private void sendCustomerNotification(Shipment shipment, ShipmentStatus status) {
        String message = switch (status) {
            case SHIPPED -> "Your order has been shipped. Tracking number: " + shipment.getTrackingNumber();
            case IN_TRANSIT -> "Your order is currently in transit.";
            case OUT_FOR_DELIVERY -> "Your order is out for delivery.";
            case DELIVERED -> "Your order has been delivered.";
            case CANCELLED -> "Your shipment has been cancelled.";
            case DELIVERY_FAILED -> "Delivery failed. Proceeding to cancel the order item.";
            case CREATED, PICKUP_SCHEDULED, PICKED_UP, RECEIVED -> null;
        };

        if (message == null) {
            return;
        }

//        String email = shipment.getOrder().getUser().getEmail();
//        notificationService.sendEmail(
//                email,
//                "Shipment update for order " + shipment.getOrder().getId(),
//                message
//        );
    }

    private void publishReturnStatusChangedAfterCommit(Return savedReturn) {
        ReturnStatusChangedEvent event = new ReturnStatusChangedEvent(savedReturn.getOrder().getId(), savedReturn.getId(),savedReturn.getOrderItem().getId(), savedReturn.getOrder().getUser().getId(), savedReturn.getStatus());
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventPublisher.publishEvent(event);
                    }
                }
        );
    }

    @Override
    public String getConsumerName() {
        return EventConsumers.SHIPMENT_LIFECYCLE;
    }
}