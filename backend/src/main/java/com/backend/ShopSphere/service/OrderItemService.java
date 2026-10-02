package com.backend.ShopSphere.service;

import com.backend.ShopSphere.constant.EntityType;
import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.entity.Return;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import com.backend.ShopSphere.enums.ReturnStatus;
import com.backend.ShopSphere.events.OrderItemCancelledEvent;
import com.backend.ShopSphere.events.OrderItemReturnEvent;
import com.backend.ShopSphere.exception.ConflictException;
import com.backend.ShopSphere.exception.ForbiddenException;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.OrderItemRepository;
import com.backend.ShopSphere.repository.ReturnRepository;
import com.backend.ShopSphere.security.SecurityUtil;
import com.backend.ShopSphere.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final SecurityUtil securityUtil;
    private final EventPublisher eventPublisher;
    private final ReturnRepository returnRepository;
    private final AuditService auditService;
    private final Helper helper;

    @Transactional
    public CancelOrderItemResponse cancelOrderItem(
            UUID orderItemId,
            CancelOrderItemRequest request) {

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order item not found"));

        Order order = orderItem.getOrder();
        FulfillmentStatus status = orderItem.getStatus();

        securityUtil.validateCurrentUser(order.getUser().getEmail());

        FulfillmentStatus currentStatus = orderItem.getStatus();

        if (!isCancellationAllowed(currentStatus)) {
            throw new IllegalArgsException(
                    "Order item cannot be cancelled in its current state: "
                            + currentStatus
            );
        }

        if (orderItem.getCancellationReason() != null) {
            throw new ConflictException(
                    "Order item has already been cancelled"
            );
        }

        orderItem.setStatus(FulfillmentStatus.CANCELLED);
        orderItem.setCancellationReason(request.getReason());
        orderItem.setCancelledAt(LocalDateTime.now());

        OrderItem saved = orderItemRepository.save(orderItem);

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
                EntityType.ORDER_ITEM,
                saved.getId(),
                status.name(),
                FulfillmentStatus.CANCELLED.name(),
                "CANCELLED",
                currentUserId,
                currentUserRole,
                request.getReason().name()
        );

        auditService.recordStatusChange(auditRequest);

        BigDecimal refundAmount = helper.calculateRefundAmount(saved);

        OrderItemCancelledEvent event = new OrderItemCancelledEvent(
                order.getId(),
                order.getUser().getId(),
                saved.getId(),
                request.getReason(),
                refundAmount
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventPublisher.publishEvent(event);
                    }
                }
        );

        log.info(
                "Order item cancelled. orderItemId={}, orderId={}, reason={}, refundAmount={}",
                saved.getId(),
                order.getId(),
                request.getReason(),
                refundAmount
        );

        return CancelOrderItemResponse.builder()
                .orderItemId(saved.getId())
                .fulfillmentStatus(saved.getStatus())
                .cancellationReason(saved.getCancellationReason())
                .refundAmount(refundAmount)
                .cancelledAt(saved.getCancelledAt())
                .build();
    }

    @Transactional
    public ReturnResponse requestReturn(UUID orderItemId, ReturnRequest request){
        log.debug("Return request arrived for order item: {}", orderItemId);
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order item not found"));

        Order order = orderItem.getOrder();

        securityUtil.validateCurrentUser(order.getUser().getEmail());
        FulfillmentStatus status = orderItem.getStatus();
        log.debug("Order item current status: {}", status);

        if (!FulfillmentStatus.DELIVERED.equals(status)) {
            throw new ForbiddenException(
                    "Return can only be requested for delivered items"
            );
        }
        LocalDateTime deliveryTime = orderItem.getDeliveredAt();
        if (deliveryTime == null) {
            throw new IllegalStateException(
                    "Delivery time not available for order item " + orderItemId
            );
        }
        int returnWindow = orderItem.getReturnWindowDays();

        if(LocalDateTime.now().isAfter(deliveryTime.plusDays(returnWindow))){
            log.warn("Return window closed. Can't proceed return.");
            throw new ForbiddenException("Return window is closed. Can't proceed with return process.");
        }

        if (returnRepository.existsByOrderItemId(orderItemId)) {
            throw new ConflictException(
                    "Return already exists for this order item"
            );
        }

        orderItem.setStatus(FulfillmentStatus.RETURN_REQUESTED);
        orderItem.setReturnedAt(LocalDateTime.now());

        Return returnRequest = Return.builder()
                .orderItem(orderItem)
                .order(order)
                .status(ReturnStatus.PICKUP_PENDING)
                .reason(request.getReason())
                .customerComment(request.getComment())
                .requestedAt(LocalDateTime.now())
                .build();

        Return saved = returnRepository.save(returnRequest);
        log.info("New return request created. Return id: {}", saved.getId());

        UUID currentUserId = securityUtil.getCurrentUserId();
        String currentUserRole = securityUtil.getCurrentUserRole();
        log.info("Current user: {}, role: {}", currentUserId, currentUserRole);

        if(currentUserRole == null || currentUserId == null){
            log.warn("Current user Id or role not found in the current context. Current userId : {}, role: {}", currentUserId, currentUserRole);
            throw new IllegalStateException(
                    "Unable to determine current user identity. Please try again later"
            );
        }

        AuditRequest orderItemAudit = new AuditRequest(
                EntityType.ORDER_ITEM,
                orderItemId,
                status.name(),
                FulfillmentStatus.RETURN_REQUESTED.name(),
                "RETURN_REQUESTED",
                currentUserId,
                currentUserRole,
                request.getReason().name()
        );

        auditService.recordStatusChange(orderItemAudit);

        AuditRequest returnAudit = new AuditRequest(
                EntityType.RETURN,
                saved.getId(),
                null,
                ReturnStatus.PICKUP_PENDING.name(),
                "RETURN_REQUESTED",
                currentUserId,
                currentUserRole,
                request.getReason().name()
        );

        auditService.recordStatusChange(returnAudit);

        OrderItemReturnEvent event = new OrderItemReturnEvent(
                saved.getId(),
                order.getId(),
                currentUserId,
                orderItemId,
                saved.getReason(),
                saved.getCustomerComment()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventPublisher.publishEvent(event);
                    }
                }
        );

        return mapToReturnResponse(saved);
    }

    private boolean isCancellationAllowed(FulfillmentStatus status) {
        return status == FulfillmentStatus.PENDING_SELLER_CONFIRMATION
                || status == FulfillmentStatus.CONFIRMED
                || status == FulfillmentStatus.PROCESSING
                || status == FulfillmentStatus.READY_FOR_PICKUP;
    }

    private ReturnResponse mapToReturnResponse(Return returnEntity){
        return ReturnResponse.builder()
                .returnId(returnEntity.getId())
                .orderId(returnEntity.getOrder().getId())
                .orderItemId(returnEntity.getOrderItem().getId())
                .customerComment(returnEntity.getCustomerComment())
                .requestedAt(returnEntity.getRequestedAt())
                .reason(returnEntity.getReason())
                .status(returnEntity.getStatus())
                .receivedAt(returnEntity.getReceivedAt())
                .approvedAt(returnEntity.getApprovedAt())
                .pickedUpAt(returnEntity.getPickedUpAt())
                .completedAt(returnEntity.getCompletedAt())
                .rejectedAt(returnEntity.getRejectedAt())
                .rejectionReason(returnEntity.getRejectionReason())
                .build();
    }
}
