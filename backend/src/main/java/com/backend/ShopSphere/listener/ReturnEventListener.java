package com.backend.ShopSphere.listener;

import com.backend.ShopSphere.constant.EntityType;
import com.backend.ShopSphere.dto.AuditRequest;
import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.entity.Refund;
import com.backend.ShopSphere.entity.Return;
import com.backend.ShopSphere.enums.*;
import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.events.OrderItemReturnEvent;
import com.backend.ShopSphere.events.RefundStatusChangedEvent;
import com.backend.ShopSphere.events.ReturnStatusChangedEvent;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.OrderItemRepository;
import com.backend.ShopSphere.repository.RefundRepository;
import com.backend.ShopSphere.repository.ReturnRepository;
import com.backend.ShopSphere.service.AuditService;
import com.backend.ShopSphere.service.EventPublisher;
import com.backend.ShopSphere.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Component
@Slf4j
@RequiredArgsConstructor
public class ReturnEventListener implements EventListener {
    private final ReturnRepository returnRepository;
    private final OrderItemRepository orderItemRepository;
    private final RefundRepository refundRepository;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;
    private final Helper helper;

    @Override
    public void process(BaseEvent event) {

        switch (event.getEventType()) {

            case ORDER_ITEM_RETURN ->
                    handleReturnRequest(
                            (OrderItemReturnEvent) event
                    );

            case RETURN_STATUS_CHANGED ->
                    handleReturnStatusChanged(
                            (ReturnStatusChangedEvent) event
                    );

            case REFUND_STATUS_CHANGED ->
                    handleRefundStatusChanged(
                            (RefundStatusChangedEvent) event
                    );

            default ->
                    log.warn(
                            "Unhandled event type for return processor: {}",
                            event.getEventType()
                    );
        }
    }

    private void handleReturnRequest(OrderItemReturnEvent event) {

        log.info(
                "Processing return request. returnId={}, orderItemId={}",
                event.getReturnId(),
                event.getOrderItemId()
        );

        Return returnEntity = returnRepository.findById(event.getReturnId()).orElseThrow(() ->
                new ResourceNotFoundException("Return not found")
        );

        log.info(
                "Return request processed. returnId={}, status={}",
                returnEntity.getId(),
                returnEntity.getStatus()
        );

        // Future:
        // seller notification / schedule shipment
    }

    private void handleReturnStatusChanged(ReturnStatusChangedEvent event) {

        log.info(
                "Return status changed. returnId={}, orderItemId={}, status={}",
                event.getReturnId(),
                event.getOrderItemId(),
                event.getStatus()
        );

        ReturnStatus status = event.getStatus();
        switch (status) {
            case APPROVED -> handleReturnApprovedReturn(event);
            case REJECTED -> handleReturnRejected(event);
            default -> log.debug(
                    "No return processor action for status={}",
                    event.getStatus()
            );
        }

        // Future:
        // pickup / inventory / refund workflow
    }

    private void handleReturnRejected(ReturnStatusChangedEvent event) {
        //to do for future


        //update order item status
        //update order item status
        OrderItem orderItem = orderItemRepository.findById(event.getOrderItemId()).orElseThrow(
                () -> new ResourceNotFoundException("Order Item not found")
        );
        FulfillmentStatus orderItemCurrentStatus = orderItem.getStatus();
        orderItem.setStatus(FulfillmentStatus.RETURN_REJECTED);
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);
        AuditRequest auditRequest = new AuditRequest(EntityType.ORDER_ITEM, savedOrderItem.getId(), orderItemCurrentStatus.name(), savedOrderItem.getStatus().name(), "Order Item status updated", null, "SYSTEM", "Return Rejected");
        auditService.recordStatusChange(auditRequest);
    }

    private void handleReturnApprovedReturn(ReturnStatusChangedEvent event){
        OrderItem orderItem = orderItemRepository.findById(event.getOrderItemId()).orElseThrow(
                () -> new ResourceNotFoundException("Order Item not found")
        );

        Return returnEntity = returnRepository.findById(event.getReturnId()).orElseThrow(
                () -> new ResourceNotFoundException("Return entity with id: " + event.getReturnId() + " not found.")
        );

        //update order item status
        FulfillmentStatus orderItemCurrentStatus = orderItem.getStatus();
        orderItem.setStatus(FulfillmentStatus.RETURN_APPROVED);
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);
        AuditRequest auditRequest = new AuditRequest(EntityType.ORDER_ITEM, savedOrderItem.getId(), orderItemCurrentStatus.name(), savedOrderItem.getStatus().name(), "Order Item status updated", null, "SYSTEM", "Return approved");
        auditService.recordStatusChange(auditRequest);

        if(refundRepository.existsByOrderItemId(event.getOrderItemId())){
            log.info("Refund already exists for orderItemId={}. Skipping refund creation.", orderItem.getId());
            return;
        }

        BigDecimal refundAmount = helper.calculateRefundAmount(orderItem);
        RefundStatus refundStatus = RefundApprovalMode.IMMEDIATE.equals(returnEntity.getRefundApprovalMode())
                        ? RefundStatus.APPROVED
                        : RefundStatus.PENDING_SELLER_APPROVAL;
        Refund refund = Refund.builder()
                .orderItem(orderItem)
                .order(orderItem.getOrder())
                .status(refundStatus)
                .amount(refundAmount)
                .requestedAt(LocalDateTime.now())
                .build();
        Refund saved = refundRepository.save(refund);

        log.info(
                "Refund created for return. returnId={}, refundId={}, status={}, amount={}",
                returnEntity.getId(),
                saved.getId(),
                saved.getStatus(),
                saved.getAmount()
        );

        ReturnStatus oldStatus = returnEntity.getStatus();
        returnEntity.setStatus(ReturnStatus.REFUND_PENDING);
        Return savedReturn = returnRepository.save(returnEntity);
        recordAudit(
                savedReturn,
                oldStatus,
                ReturnStatus.REFUND_PENDING,
                "REFUND_REQUESTED"
        );
        publishReturnStatusChangedAfterCommit(savedReturn);
        publishRefundStatusChangedAfterCommit(saved);
    }

    private void handleRefundStatusChanged(RefundStatusChangedEvent event){
        if (!RefundStatus.REFUNDED.equals(event.getStatus())) {
            return;
        }

        Return returnEntity = returnRepository.findByOrderItemId(event.getOrderItemId()).orElseThrow(
                () -> new ResourceNotFoundException("Return not found for order item: " + event.getOrderItemId())
        );

        if(ReturnStatus.COMPLETED.equals(returnEntity.getStatus())){
            log.info("Return already completed. returnId={}", returnEntity.getId());
            return;
        }

        ReturnStatus oldStatus = returnEntity.getStatus();
        returnEntity.setStatus(ReturnStatus.COMPLETED);
        returnEntity.setCompletedAt(LocalDateTime.now());

        Return saved = returnRepository.save(returnEntity);
        log.info(
                "Return completed after refund. returnId={}, {} -> {}",
                saved.getId(),
                oldStatus,
                saved.getStatus()
        );

        recordAudit(saved, oldStatus, ReturnStatus.COMPLETED, "REFUND_COMPLETED");

        //update order item status
        OrderItem orderItem = orderItemRepository.findById(event.getOrderItemId()).orElseThrow(
                () -> new ResourceNotFoundException("Order Item not found")
        );
        FulfillmentStatus orderItemCurrentStatus = orderItem.getStatus();
        orderItem.setStatus(FulfillmentStatus.RETURN_COMPLETED);
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);
        AuditRequest auditRequest = new AuditRequest(EntityType.ORDER_ITEM, savedOrderItem.getId(), orderItemCurrentStatus.name(), savedOrderItem.getStatus().name(), "Order Item status updated", null, "SYSTEM", "Refund completed");
        auditService.recordStatusChange(auditRequest);

        publishReturnStatusChangedAfterCommit(saved);
    }

    private void recordAudit(Return returnEntity, ReturnStatus oldStatus, ReturnStatus newStatus, String action) {
        AuditRequest auditRequest = new AuditRequest(
                EntityType.RETURN,
                returnEntity.getId(),
                oldStatus != null
                        ? oldStatus.name()
                        : null,
                newStatus.name(),
                action,
                null,
                "SYSTEM",
                null
        );

        auditService.recordStatusChange(auditRequest);
    }

    private void publishReturnStatusChangedAfterCommit(
            Return returnEntity
    ) {
        ReturnStatusChangedEvent event =
                new ReturnStatusChangedEvent(
                        returnEntity.getOrder().getId(),
                        returnEntity.getId(),
                        returnEntity.getOrderItem().getId(),
                        returnEntity.getOrder().getUser().getId(),
                        returnEntity.getStatus()
                );

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {
                                eventPublisher.publishEvent(event);
                            }
                        }
                );
    }

    private void publishRefundStatusChangedAfterCommit(Refund refund) {
        RefundStatusChangedEvent event =
                new RefundStatusChangedEvent(
                        refund.getOrder().getId(),
                        refund.getId(),
                        refund.getOrderItem().getId(),
                        refund.getOrder().getUser().getId(),
                        refund.getStatus(),
                        refund.getAmount()
                );

        TransactionSynchronizationManager
                .registerSynchronization(
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
        return EventConsumers.RETURN_PROCESSOR;
    }
}