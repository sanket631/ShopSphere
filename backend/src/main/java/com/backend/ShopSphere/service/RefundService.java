package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.entity.Payment;
import com.backend.ShopSphere.entity.Refund;
import com.backend.ShopSphere.entity.SellerOrderItem;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.enums.PaymentStatus;
import com.backend.ShopSphere.enums.RefundStatus;
import com.backend.ShopSphere.events.RefundStatusChangedEvent;
import com.backend.ShopSphere.exception.ForbiddenException;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.PaymentRepository;
import com.backend.ShopSphere.repository.RefundRepository;
import com.backend.ShopSphere.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final SecurityUtil securityUtil;
    private final EventPublisher eventPublisher;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;

    @Transactional
    public RefundResponse updateRefundStatus(UUID refundId, RefundStatusUpdateRequest request){
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Refund not found"));

        SellerOrderItem sellerOrderItem = refund.getOrderItem().getSellerOrderItem();

        if (sellerOrderItem == null) {
            throw new IllegalStateException(
                    "Seller order item not found for refund"
            );
        }
        User seller = sellerOrderItem.getSeller();
        securityUtil.validateCurrentUser(seller.getEmail());

        RefundStatus currentStatus = refund.getStatus();
        RefundStatus nextStatus = request.getStatus();

        if (!RefundStatus.PENDING_SELLER_APPROVAL.equals(currentStatus)) {
            throw new ForbiddenException(
                    "You are not allowed to perform this action"
            );
        }

        validateTransition(currentStatus, nextStatus);

        refund.setStatus(nextStatus);
        AuditRequest auditRequest = new AuditRequest(
                "REFUND",
                refund.getId(),
                currentStatus.name(),
                nextStatus.name(),
                "STATUS_CHANGED",
                seller.getId(),
                "SELLER",
                request.getReason()
        );

        auditService.recordStatusChange(auditRequest);

        if (RefundStatus.APPROVED.equals(nextStatus)) {
            refund.setApprovedAt(LocalDateTime.now());
        }

        if (RefundStatus.REJECTED.equals(nextStatus)) {
            String reason = request.getReason();
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgsException(
                        "Provide reason for rejecting the request"
                );
            }
            refund.setRejectedAt(LocalDateTime.now());
            refund.setRejectedReason(reason);
        }

        Refund saved = refundRepository.save(refund);

        publishRefundStatusChangedEvent(saved);

        log.info(
                "Refund status updated. refundId={}, orderId={}, orderItemId={}, oldStatus={}, newStatus={}",
                saved.getId(),
                saved.getOrder().getId(),
                saved.getOrderItem().getId(),
                currentStatus,
                nextStatus
        );

        return mapToResponse(saved);
    }

    private void validateTransition(RefundStatus currentStatus, RefundStatus nextStatus) {
        if(currentStatus.equals(nextStatus)){
            throw new IllegalArgsException("New refund status is equal to the current refund status. Provide a different refund status");
        }

        Set<RefundStatus> allowedStatuses = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowedStatuses.contains(nextStatus)) {
            throw new IllegalArgsException(
                    "Invalid refund status transition from "
                            + currentStatus + " to " + nextStatus
            );
        }
    }

    private static final Map<RefundStatus, Set<RefundStatus>> VALID_TRANSITIONS =
            Map.of(
                    RefundStatus.PENDING_SELLER_APPROVAL,
                    Set.of(
                            RefundStatus.APPROVED,
                            RefundStatus.REJECTED
                    ),
                    RefundStatus.APPROVED,
                    Set.of(RefundStatus.PROCESSING),
                    RefundStatus.PROCESSING,
                    Set.of(
                            RefundStatus.REFUNDED,
                            RefundStatus.FAILED
                    ),
                    RefundStatus.FAILED,
                    Set.of(RefundStatus.PROCESSING),
                    RefundStatus.REJECTED,
                    Set.of(),
                    RefundStatus.REFUNDED,
                    Set.of()
            );

    @Transactional
    public void processRefund(UUID refundId) {

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Refund not found"));

        RefundStatus status = refund.getStatus();
        if (!RefundStatus.APPROVED.equals(status)) {
            log.warn(
                    "Refund is not approved for processing. refundId={}, status={}",
                    refundId,
                    refund.getStatus()
            );
            return;
        }

        Payment payment = paymentRepository
                .findByOrderId(refund.getOrder().getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        if (!PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            throw new IllegalArgsException(
                    "Refund cannot be processed because payment status is "
                            + payment.getStatus()
            );
        }

        refund.setStatus(RefundStatus.PROCESSING);

        AuditRequest processingAudit = new AuditRequest(
                "REFUND",
                refund.getId(),
                status.name(),
                RefundStatus.PROCESSING.name(),
                "STATUS_CHANGED",
                null,
                "SYSTEM",
                null
        );
        auditService.recordStatusChange(processingAudit);

        refund.setProcessedAt(LocalDateTime.now());
        Refund processingRefund = refundRepository.save(refund);
        publishRefundStatusChangedEvent(processingRefund);

        log.info(
                "Refund moved to PROCESSING. refundId={}, orderId={}, amount={}",
                refund.getId(),
                refund.getOrder().getId(),
                refund.getAmount()
        );

        PaymentGatewayResponse gatewayResponse = paymentGateway.refundPayment(payment.getTransactionId(), refund.getAmount());

        if (gatewayResponse.isSuccessful()) {

            refund.setStatus(RefundStatus.REFUNDED);
            refund.setRefundTransactionId(
                    gatewayResponse.getTransactionId()
            );
            refund.setRefundedAt(LocalDateTime.now());

            AuditRequest refundAuditRequest = new AuditRequest(
                    "REFUND",
                    refund.getId(),
                    status.name(),
                    RefundStatus.REFUNDED.name(),
                    "REFUND_COMPLETED",
                    null,
                    "SYSTEM",
                    null
            );
            auditService.recordStatusChange(refundAuditRequest);

            log.info(
                    "Refund completed successfully. refundId={}, orderId={}, amount={}, refundTransactionId={}",
                    refund.getId(),
                    refund.getOrder().getId(),
                    refund.getAmount(),
                    refund.getRefundTransactionId()
            );

        } else {

            refund.setStatus(RefundStatus.FAILED);

            log.error(
                    "Refund failed. refundId={}, orderId={}, amount={}, message={}",
                    refund.getId(),
                    refund.getOrder().getId(),
                    refund.getAmount(),
                    gatewayResponse.getMessage()
            );
        }

        Refund saved = refundRepository.save(refund);
        publishRefundStatusChangedEvent(saved);
        log.info("Refund {} completed with status {}", refundId, saved.getStatus());
    }

    private RefundResponse mapToResponse(Refund refund){
        return RefundResponse.builder()
                .orderId(refund.getOrder().getId())
                .refundId(refund.getId())
                .amount(refund.getAmount())
                .orderItemId(refund.getOrderItem().getId())
                .status(refund.getStatus())
                .refundedAt(refund.getRefundedAt())
                .approvedAt(refund.getApprovedAt())
                .requestedAt(refund.getRequestedAt())
                .rejectedAt(refund.getRejectedAt())
                .processedAt(refund.getProcessedAt())
                .refundTransactionId(refund.getRefundTransactionId())
                .productVariantId(refund.getOrderItem().getProductVariantId())
                .sku(refund.getOrderItem().getSku())
                .quantity(refund.getOrderItem().getQuantity())
                .build();
    }

    private void publishRefundStatusChangedEvent(Refund refund) {
        SellerOrderItem sellerOrderItem = refund.getOrderItem().getSellerOrderItem();
        if (sellerOrderItem == null) {
            throw new IllegalStateException(
                    "Seller order item not found for refund " + refund.getId());
        }

        RefundStatusChangedEvent event = new RefundStatusChangedEvent(
                refund.getOrder().getId(),
                refund.getId(),
                sellerOrderItem.getOrderItem().getId(),
                refund.getOrder().getUser().getId(),
                refund.getStatus(),
                refund.getAmount()
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

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> getSellerRefunds(int page, int size) {
        UUID sellerId = securityUtil.getCurrentUserId();

        log.debug(
                "Fetching seller refunds. sellerId={}, page={}, size={}",
                sellerId,
                page,
                size
        );

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "requestedAt"
                )
        );

        Page<Refund> refunds = refundRepository.findSellerRefunds(
                        sellerId,
                        pageable
                );

        log.info(
                "Retrieved seller refunds. sellerId={}, count={}, total={}",
                sellerId,
                refunds.getNumberOfElements(),
                refunds.getTotalElements()
        );

        return mapToPageResponse(refunds);
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> getPendingSellerRefunds(int page, int size) {
        UUID sellerId = securityUtil.getCurrentUserId();

        log.debug(
                "Fetching pending seller refunds. sellerId={}, page={}, size={}",
                sellerId,
                page,
                size
        );

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.ASC,
                        "requestedAt"
                )
        );

        Page<Refund> refunds = refundRepository.findSellerRefundsByStatus(sellerId, RefundStatus.PENDING_SELLER_APPROVAL, pageable);

        log.info(
                "Retrieved pending seller refunds. sellerId={}, count={}, total={}",
                sellerId,
                refunds.getNumberOfElements(),
                refunds.getTotalElements()
        );

        return mapToPageResponse(refunds);
    }

    private PageResponse<RefundResponse> mapToPageResponse(
            Page<Refund> refunds
    ) {
        return PageResponse.<RefundResponse>builder()
                .content(
                        refunds.getContent()
                                .stream()
                                .map(this::mapToResponse)
                                .toList()
                )
                .page(refunds.getNumber())
                .size(refunds.getSize())
                .totalElements(refunds.getTotalElements())
                .totalPages(refunds.getTotalPages())
                .first(refunds.isFirst())
                .last(refunds.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public RefundResponse getSellerRefund(UUID refundId) {

        log.debug(
                "Fetching seller refund. refundId={}",
                refundId
        );

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Refund not found"
                        )
                );

        securityUtil.validateCurrentUser(
                refund.getOrderItem()
                        .getSellerOrderItem()
                        .getSeller()
                        .getEmail()
        );

        return mapToResponse(refund);
    }
}
