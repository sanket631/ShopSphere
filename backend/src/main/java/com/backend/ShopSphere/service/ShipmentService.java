package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.AuditRequest;
import com.backend.ShopSphere.dto.ReturnShipmentResponse;
import com.backend.ShopSphere.dto.ShipmentResponse;
import com.backend.ShopSphere.dto.ShippingAddressResponse;
import com.backend.ShopSphere.entity.*;
import com.backend.ShopSphere.enums.*;
import com.backend.ShopSphere.events.OrderItemCancelledEvent;
import com.backend.ShopSphere.events.ReturnStatusChangedEvent;
import com.backend.ShopSphere.events.ShipmentStatusChangedEvent;
import com.backend.ShopSphere.exception.ConflictException;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.OrderRepository;
import com.backend.ShopSphere.repository.ReturnRepository;
import com.backend.ShopSphere.repository.SellerOrderRepository;
import com.backend.ShopSphere.repository.ShipmentRepository;
import com.backend.ShopSphere.security.SecurityUtil;
import com.backend.ShopSphere.constant.EntityType;
import com.backend.ShopSphere.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final SellerOrderRepository sellerOrderRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtil securityUtil;
    private final EventPublisher eventPublisher;
    private final ReturnRepository returnRepository;
    private final AuditService auditService;
    private final Helper helper;

    @Transactional
    public ShipmentResponse createShipment(UUID sellerOrderItemId, String carrier) {
        SellerOrderItem sellerOrderItem = sellerOrderRepository.findById(sellerOrderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller order item not found"));

        securityUtil.validateCurrentUser(sellerOrderItem.getSeller().getEmail());

        if (!FulfillmentStatus.READY_FOR_PICKUP.equals(sellerOrderItem.getStatus())) {
            throw new IllegalArgsException("Seller order item is not ready for shipment");
        }

        if (shipmentRepository.existsBySellerOrderItemIdAndType(sellerOrderItemId, ShipmentType.FORWARD)) {
            throw new ConflictException(
                    "Forward shipment already exists for this seller order item"
            );
        }


        Order order = sellerOrderItem.getOrder();

        Shipment shipment = Shipment.builder()
                .sellerOrderItem(sellerOrderItem)
                .order(order)
                .seller(sellerOrderItem.getSeller())
                .type(ShipmentType.FORWARD)
                .trackingNumber(generateTrackingNumber())
                .carrier(carrier)
                .status(ShipmentStatus.CREATED)
                .build();

        Shipment saved = shipmentRepository.save(shipment);

        log.info(
                "Shipment created. shipmentId={}, orderId={}, sellerOrderItemId={}, sellerId={}, trackingNumber={}",
                saved.getId(),
                order.getId(),
                sellerOrderItem.getId(),
                sellerOrderItem.getSeller().getId(),
                saved.getTrackingNumber()
        );

        return mapToShipmentResponse(saved);
    }

    @Transactional
    public ReturnShipmentResponse createReturnShipment(UUID returnId, String carrier) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Return not found"));

        ReturnStatus currentStatus = returnEntity.getStatus();

        if (!ReturnStatus.PICKUP_PENDING.equals(currentStatus)) {
            throw new IllegalArgsException(
                    "Return is not pending pickup"
            );
        }

        OrderItem orderItem = returnEntity.getOrderItem();

        SellerOrderItem sellerOrderItem = orderItem.getSellerOrderItem();

        if (sellerOrderItem == null) {
            throw new IllegalStateException(
                    "Seller order item not found for return"
            );
        }

        if (shipmentRepository.existsBySellerOrderItemIdAndType(sellerOrderItem.getId(), ShipmentType.RETURN)) {
            throw new ConflictException(
                    "Return shipment already exists"
            );
        }

        Order order = returnEntity.getOrder();

        Shipment shipment = Shipment.builder()
                .sellerOrderItem(sellerOrderItem)
                .order(order)
                .seller(sellerOrderItem.getSeller())
                .type(ShipmentType.RETURN)
                .trackingNumber(generateTrackingNumber())
                .carrier(carrier)
                .status(ShipmentStatus.PICKUP_SCHEDULED)
                .pickupScheduledAt(LocalDateTime.now())
                .build();

        Shipment saved = shipmentRepository.save(shipment);

        ReturnStatus oldStatus = returnEntity.getStatus();

        returnEntity.setStatus(ReturnStatus.PICKUP_SCHEDULED);

        Return savedReturn = returnRepository.save(returnEntity);

        log.info(
                "Return pickup scheduled. returnId={}, shipmentId={}, orderItemId={}, trackingNumber={}",
                savedReturn.getId(),
                saved.getId(),
                orderItem.getId(),
                saved.getTrackingNumber()
        );

        UUID currentUserId = securityUtil.getCurrentUserId();
        String currentUserRole = securityUtil.getCurrentUserRole();
        log.info("Current user: {}, role: {}", currentUserId, currentUserRole);

        if(currentUserRole == null || currentUserId == null){
            log.warn("Current user Id or role not found in the current context. Current userId : {}, role: {}", currentUserId, currentUserRole);
            throw new IllegalStateException(
                    "Unable to determine current user identity. Please try again later"
            );
        }

        AuditRequest auditRequest = new AuditRequest(
                EntityType.RETURN,
                savedReturn.getId(),
                oldStatus.name(),
                ReturnStatus.PICKUP_SCHEDULED.name(),
                "RETURN_SHIPMENT_CREATED",
                currentUserId,
                currentUserRole,
                "Return shipment created"
        );
        auditService.recordStatusChange(auditRequest);

        publishReturnStatusChangedAfterCommit(savedReturn);

        return mapToReturnShipmentResponse(saved);
    }

    @Transactional
    public ShipmentResponse updateForwardShipmentStatus(UUID shipmentId, ShipmentStatus nextStatus){
        return mapToShipmentResponse(updateShipmentStatus(shipmentId,nextStatus));
    }

    @Transactional
    public ReturnShipmentResponse updateReturnShipmentStatus(UUID shipmentId, ShipmentStatus nextStatus){
        Shipment updatedShipment = updateShipmentStatus(shipmentId, nextStatus);
        return mapToReturnShipmentResponse(updatedShipment);
    }

    private Shipment updateShipmentStatus(UUID shipmentId, ShipmentStatus nextStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        securityUtil.validateCurrentUser(shipment.getSeller().getEmail());

        ShipmentStatus currentStatus = shipment.getStatus();

        if (!isValidTransition(shipment.getType(), currentStatus, nextStatus)) {
            throw new IllegalArgsException(
                    "Invalid shipment status transition from "
                            + currentStatus + " to " + nextStatus
            );
        }

        shipment.setStatus(nextStatus);

        LocalDateTime now = LocalDateTime.now();

        if (ShipmentStatus.SHIPPED.equals(nextStatus)) {
            shipment.setShippedAt(now);
        }

        if (ShipmentStatus.DELIVERED.equals(nextStatus)) {
            shipment.setDeliveredAt(now);
        }

        if (ShipmentStatus.PICKED_UP.equals(nextStatus)) {
            shipment.setPickedUpAt(now);
        }

        if (ShipmentStatus.RECEIVED.equals(nextStatus)) {
            shipment.setReceivedAt(now);
        }

        Shipment saved = shipmentRepository.save(shipment);

        log.info(
                "Shipment status updated. shipmentId={}, from={}, to={}",
                shipmentId,
                currentStatus,
                nextStatus
        );

        ShipmentStatusChangedEvent event = new ShipmentStatusChangedEvent(
                shipment.getOrder().getId(),
                shipment.getOrder().getUser().getId(),
                shipment.getId(),
                currentStatus,
                nextStatus,
                shipment.getType()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventPublisher.publishEvent(event);
                    }
                }
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipment(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        securityUtil.validateCurrentUser(shipment.getOrder().getUser().getEmail());

        return mapToShipmentResponse(shipment);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getOrderShipments(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        securityUtil.validateCurrentUser(order.getUser().getEmail());

        return shipmentRepository.findByOrder(order)
                .stream()
                .map(this::mapToShipmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShipmentResponse trackShipment(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        securityUtil.validateCurrentUser(shipment.getOrder().getUser().getEmail());

        return mapToShipmentResponse(shipment);
    }

    @Transactional
    public void cancelAfterDeliveryFailure(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        securityUtil.validateCurrentUser(shipment.getSeller().getEmail());

        if (!ShipmentType.FORWARD.equals(shipment.getType())) {
            throw new IllegalArgsException(
                    "Delivery failure cancellation is only valid for forward shipments"
            );
        }

        if (!ShipmentStatus.DELIVERY_FAILED.equals(shipment.getStatus())) {
            throw new IllegalArgsException(
                    "Shipment cannot be cancelled because delivery has not failed"
            );
        }

        SellerOrderItem sellerOrderItem = shipment.getSellerOrderItem();

        if (FulfillmentStatus.CANCELLED.equals(sellerOrderItem.getStatus())) {
            throw new ConflictException("Order item is already cancelled");
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipmentRepository.save(shipment);

        OrderItem orderItem = sellerOrderItem.getOrderItem();
        Order order = orderItem.getOrder();

        BigDecimal refundAmount = helper.calculateRefundAmount(orderItem);

        orderItem.setStatus(FulfillmentStatus.CANCELLED);
        orderItem.setCancellationReason(CancellationReason.DELIVERY_FAILED);
        orderItem.setCancelledAt(LocalDateTime.now());

        log.info(
                "Order item cancelled after delivery failure. orderItemId={}, shipmentId={}, refundAmount={}",
                orderItem.getId(),
                shipmentId,
                refundAmount
        );

        OrderItemCancelledEvent event = new OrderItemCancelledEvent(
                order.getId(),
                order.getUser().getId(),
                orderItem.getId(),
                CancellationReason.DELIVERY_FAILED,
                refundAmount
        );
        eventPublisher.publishEvent(event);
    }

    private ReturnStatus mapToReturnStatus(ShipmentStatus shipmentStatus) {
        return switch (shipmentStatus) {
            case PICKED_UP -> ReturnStatus.PICKED_UP;
            case RECEIVED -> ReturnStatus.RECEIVED;
            default -> null;
        };
    }

    private boolean isValidTransition(ShipmentType type, ShipmentStatus current, ShipmentStatus next) {
        if (ShipmentType.FORWARD.equals(type)) {
            return isValidForwardTransition(current, next);
        }

        return isValidReturnTransition(current, next);
    }

    private boolean isValidForwardTransition(ShipmentStatus current, ShipmentStatus next) {
        return switch (current) {
            case CREATED ->
                    next == ShipmentStatus.SHIPPED
                            || next == ShipmentStatus.CANCELLED;

            case SHIPPED ->
                    next == ShipmentStatus.IN_TRANSIT;

            case IN_TRANSIT ->
                    next == ShipmentStatus.OUT_FOR_DELIVERY;

            case OUT_FOR_DELIVERY ->
                    next == ShipmentStatus.DELIVERY_FAILED
                            || next == ShipmentStatus.DELIVERED;

            case DELIVERY_FAILED ->
                    next == ShipmentStatus.CANCELLED;

            case DELIVERED,
                 PICKUP_SCHEDULED,
                 PICKED_UP,
                 RECEIVED,
                 CANCELLED ->
                    false;
        };
    }

    private boolean isValidReturnTransition(ShipmentStatus current, ShipmentStatus next) {
        return switch (current) {
            case PICKUP_SCHEDULED ->
                    next == ShipmentStatus.PICKED_UP
                            || next == ShipmentStatus.CANCELLED;

            case PICKED_UP -> next == ShipmentStatus.RECEIVED;

            case CREATED,
                 SHIPPED,
                 IN_TRANSIT,
                 OUT_FOR_DELIVERY,
                 DELIVERED,
                 DELIVERY_FAILED,
                 RECEIVED,
                 CANCELLED -> false;
        };
    }

    private String generateTrackingNumber() {
        return "SS-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();
    }

    private ShipmentResponse mapToShipmentResponse(Shipment shipment) {
        SellerOrderItem sellerOrderItem = shipment.getSellerOrderItem();

        return ShipmentResponse.builder()
                .shipmentId(shipment.getId())
                .type(shipment.getType())
                .orderId(shipment.getOrder().getId())
                .sellerOrderItemId(sellerOrderItem.getId())
                .sellerId(shipment.getSeller().getId())
                .productVariantId(sellerOrderItem.getProductVariantId())
                .sku(sellerOrderItem.getSku())
                .quantity(sellerOrderItem.getQuantity())
                .trackingNumber(shipment.getTrackingNumber())
                .carrier(shipment.getCarrier())
                .shippingMethod(shipment.getOrder().getShippingMethod())
                .shippingCharge(shipment.getOrder().getShippingCharge())
                .estimatedDeliveryDate(shipment.getOrder().getEstimatedDeliveryDate())
                .status(shipment.getStatus())
                .shippedAt(shipment.getShippedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .shippingAddress(mapShippingAddress(shipment.getOrder()))
                .build();
    }

    private ReturnShipmentResponse mapToReturnShipmentResponse(Shipment shipment){
        return ReturnShipmentResponse.builder()
                .shipmentId(shipment.getId())
                .type(shipment.getType())
                .returnId(shipment.getSellerOrderItem().getOrderItem().getReturnEntity().getId())
                .orderId(shipment.getOrder().getId())
                .orderItemId(shipment.getSellerOrderItem().getOrderItem().getId())
                .sellerOrderItemId(shipment.getSellerOrderItem().getId())
                .productVariantId(shipment.getSellerOrderItem().getProductVariantId())
                .sku(shipment.getSellerOrderItem().getSku())
                .quantity(shipment.getSellerOrderItem().getQuantity())
                .trackingNumber(shipment.getTrackingNumber())
                .carrier(shipment.getCarrier())
                .status(shipment.getStatus())
                .pickupScheduledAt(shipment.getPickupScheduledAt())
                .pickedUpAt(shipment.getPickedUpAt())
                .receivedAt(shipment.getReceivedAt())
                .build();
    }

    private ShippingAddressResponse mapShippingAddress(Order order) {
        log.trace("Mapping shipping address from order - Order ID: {}", order.getId());

        ShippingAddressResponse response = ShippingAddressResponse.builder()
                .fullName(order.getShippingFullName())
                .phoneNumber(order.getShippingPhoneNumber())
                .addressLine1(order.getShippingAddressLine1())
                .addressLine2(order.getShippingAddressLine2())
                .landmark(order.getShippingLandmark())
                .city(order.getShippingCity())
                .state(order.getShippingState())
                .postalCode(order.getShippingPostalCode())
                .country(order.getShippingCountry())
                .build();

        log.trace("Shipping address mapped - Name: {}, City: {}",
                response.getFullName(), response.getCity());
        return response;
    }

    private void publishReturnStatusChangedAfterCommit(Return savedReturn) {
        ReturnStatusChangedEvent event = new ReturnStatusChangedEvent(
                savedReturn.getOrder().getId(),
                savedReturn.getId(),
                savedReturn.getOrderItem().getId(),
                savedReturn.getOrder().getUser().getId(),
                savedReturn.getStatus()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventPublisher.publishEvent(event);
                    }
                }
        );
    }
}