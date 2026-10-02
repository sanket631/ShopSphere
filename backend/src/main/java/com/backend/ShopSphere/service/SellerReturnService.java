package com.backend.ShopSphere.service;

import com.backend.ShopSphere.constant.EntityType;
import com.backend.ShopSphere.dto.AuditRequest;
import com.backend.ShopSphere.dto.PageResponse;
import com.backend.ShopSphere.dto.ReturnStatusUpdateRequest;
import com.backend.ShopSphere.dto.SellerReturnResponse;
import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.entity.Return;
import com.backend.ShopSphere.enums.RefundApprovalMode;
import com.backend.ShopSphere.enums.ReturnStatus;
import com.backend.ShopSphere.events.ReturnStatusChangedEvent;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.ReturnRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerReturnService {
    private final ReturnRepository returnRepository;
    private final SecurityUtil securityUtil;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<SellerReturnResponse> getAllReturns(int page, int size) {
        UUID sellerId = securityUtil.getCurrentUserId();

        log.debug(
                "Fetching seller returns. sellerId={}, page={}, size={}",
                sellerId,
                page,
                size
        );

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "requestedAt")
        );

        Page<Return> returns = returnRepository.findSellerReturns(sellerId, pageable);

        log.info(
                "Retrieved seller returns. sellerId={}, count={}, total={}",
                sellerId,
                returns.getNumberOfElements(),
                returns.getTotalElements()
        );

        return mapToPageResponse(returns);
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerReturnResponse> getPendingReturns(int page, int size) {
        UUID sellerId = securityUtil.getCurrentUserId();

        log.debug(
                "Fetching pending seller returns. sellerId={}, page={}, size={}",
                sellerId,
                page,
                size
        );

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "receivedAt")
        );

        Page<Return> returns =
                returnRepository.findSellerReturnsByStatus(
                        sellerId,
                        ReturnStatus.RECEIVED,
                        pageable
                );

        log.info(
                "Retrieved pending seller returns. sellerId={}, count={}, total={}",
                sellerId,
                returns.getNumberOfElements(),
                returns.getTotalElements()
        );

        return mapToPageResponse(returns);
    }

    @Transactional(readOnly = true)
    public SellerReturnResponse getReturn(UUID returnId) {

        log.debug(
                "Fetching seller return details. returnId={}",
                returnId
        );

        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Return not found"
                        )
                );

        securityUtil.validateCurrentUser(
                returnEntity
                        .getOrderItem()
                        .getSellerOrderItem()
                        .getSeller()
                        .getEmail()
        );

        return mapToResponse(returnEntity);
    }

    @Transactional
    public SellerReturnResponse updateReturnStatus(
            UUID returnId,
            ReturnStatusUpdateRequest request
    ) {
        log.debug(
                "Seller return status update requested. returnId={}, requestedStatus={}",
                returnId,
                request.getStatus()
        );

        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Return not found"
                        )
                );

        securityUtil.validateCurrentUser(
                returnEntity
                        .getOrderItem()
                        .getSellerOrderItem()
                        .getSeller()
                        .getEmail()
        );

        ReturnStatus currentStatus = returnEntity.getStatus();
        ReturnStatus requestedStatus = request.getStatus();

        if (!isValidSellerTransition(
                currentStatus,
                requestedStatus
        )) {
            log.warn(
                    "Invalid seller return transition. returnId={}, {} -> {}",
                    returnId,
                    currentStatus,
                    requestedStatus
            );

            throw new IllegalArgsException(
                    "Invalid return transition: "
                            + currentStatus
                            + " -> "
                            + requestedStatus
            );
        }

        validateRequest(request);

        returnEntity.setStatus(requestedStatus);

        if (ReturnStatus.APPROVED.equals(requestedStatus)) {
            returnEntity.setApprovedAt(LocalDateTime.now());

            returnEntity.setRefundApprovalMode(
                    Boolean.TRUE.equals(
                            request.getApproveRefundImmediately()
                    )
                            ? RefundApprovalMode.IMMEDIATE
                            : RefundApprovalMode.SELLER_APPROVAL
            );
        }

        if (ReturnStatus.REJECTED.equals(requestedStatus)) {
            returnEntity.setRejectedAt(LocalDateTime.now());
            returnEntity.setRejectionReason(request.getReason());
        }

        Return saved = returnRepository.save(returnEntity);

        log.info(
                "Seller return status updated. returnId={}, {} -> {}",
                returnId,
                currentStatus,
                requestedStatus
        );

        recordAudit(
                saved,
                currentStatus,
                requestedStatus,
                request
        );

        publishStatusChangedEvent(saved);

        return mapToResponse(saved);
    }

    private boolean isValidSellerTransition(
            ReturnStatus current,
            ReturnStatus next
    ) {
        return current == ReturnStatus.RECEIVED
                && (
                next == ReturnStatus.APPROVED
                        || next == ReturnStatus.REJECTED
        );
    }

    private void validateRequest(
            ReturnStatusUpdateRequest request
    ) {
        if (ReturnStatus.REJECTED.equals(request.getStatus())) {

            if (request.getReason() == null
                    || request.getReason().isBlank()) {

                throw new IllegalArgsException(
                        "Reason is required when rejecting a return"
                );
            }
        }

        if (ReturnStatus.APPROVED.equals(request.getStatus())) {

            if (request.getApproveRefundImmediately() == null) {

                throw new IllegalArgsException(
                        "approveRefundImmediately is required when approving a return"
                );
            }
        }
    }

    private void recordAudit(
            Return returnEntity,
            ReturnStatus oldStatus,
            ReturnStatus newStatus,
            ReturnStatusUpdateRequest request
    ) {
        UUID userId = securityUtil.getCurrentUserId();
        String role = securityUtil.getCurrentUserRole();

        AuditRequest auditRequest = new AuditRequest(
                EntityType.RETURN,
                returnEntity.getId(),
                oldStatus.name(),
                newStatus.name(),
                "RETURN_STATUS_CHANGED",
                userId,
                role,
                request.getReason()
        );

        auditService.recordStatusChange(auditRequest);
    }

    private void publishStatusChangedEvent(Return returnEntity) {

        ReturnStatusChangedEvent event =
                new ReturnStatusChangedEvent(
                        returnEntity.getOrder().getId(),
                        returnEntity.getId(),
                        returnEntity.getOrderItem().getId(),
                        returnEntity.getOrder().getUser().getId(),
                        returnEntity.getStatus()
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

    private PageResponse<SellerReturnResponse> mapToPageResponse(
            Page<Return> returns
    ) {
        return PageResponse.<SellerReturnResponse>builder()
                .content(
                        returns.getContent()
                                .stream()
                                .map(this::mapToResponse)
                                .toList()
                )
                .page(returns.getNumber())
                .size(returns.getSize())
                .totalElements(returns.getTotalElements())
                .totalPages(returns.getTotalPages())
                .first(returns.isFirst())
                .last(returns.isLast())
                .build();
    }

    private SellerReturnResponse mapToResponse(Return returnEntity) {

        OrderItem orderItem = returnEntity.getOrderItem();

        return SellerReturnResponse.builder()
                .returnId(returnEntity.getId())
                .orderId(returnEntity.getOrder().getId())
                .orderItemId(orderItem.getId())
                .productVariantId(orderItem.getProductVariantId())
                .sku(orderItem.getSku())
                .quantity(orderItem.getQuantity())
                .status(returnEntity.getStatus())
                .reason(returnEntity.getReason())
                .customerComment(returnEntity.getCustomerComment())
                .requestedAt(returnEntity.getRequestedAt())
                .pickedUpAt(returnEntity.getPickedUpAt())
                .receivedAt(returnEntity.getReceivedAt())
                .approvedAt(returnEntity.getApprovedAt())
                .rejectedAt(returnEntity.getRejectedAt())
                .completedAt(returnEntity.getCompletedAt())
                .rejectionReason(returnEntity.getRejectionReason())
                .build();
    }
}
