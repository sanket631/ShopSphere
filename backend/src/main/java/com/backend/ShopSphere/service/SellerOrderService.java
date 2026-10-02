package com.backend.ShopSphere.service;

import com.backend.ShopSphere.constant.EntityType;
import com.backend.ShopSphere.dto.AuditRequest;
import com.backend.ShopSphere.dto.PageResponse;
import com.backend.ShopSphere.dto.SellerOrderResponse;
import com.backend.ShopSphere.dto.SellerOrderUpdateRequest;
import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.entity.SellerOrderItem;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.OrderItemRepository;
import com.backend.ShopSphere.repository.SellerOrderRepository;
import com.backend.ShopSphere.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerOrderService {
    private final SellerOrderRepository sellerOrderRepository;
    private final SecurityUtil securityUtil;
    private final AuditService auditService;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public PageResponse<SellerOrderResponse> getAllOrderProducts(int page, int size) {
        log.debug("Request received to fetch seller orders. page={}, size={}", page, size);

        UUID sellerId = securityUtil.getCurrentUserId();
        log.debug("Fetching seller orders for sellerId={}", sellerId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<SellerOrderItem> orderItems = sellerOrderRepository.findBySeller_Id(sellerId, pageable);

        log.info(
                "Retrieved seller orders. sellerId={}, page={}, size={}, totalElements={}",
                sellerId,
                page,
                size,
                orderItems.getTotalElements()
        );

        return PageResponse.<SellerOrderResponse>builder()
                .content(orderItems.getContent()
                        .stream()
                        .map(this::mapToSellerOrderResponse)
                        .toList())
                .page(orderItems.getNumber())
                .size(orderItems.getSize())
                .totalElements(orderItems.getTotalElements())
                .totalPages(orderItems.getTotalPages())
                .first(orderItems.isFirst())
                .last(orderItems.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerOrderResponse> getOrdersByStatus(FulfillmentStatus status, int page, int size) {
        log.debug(
                "Request received to fetch seller orders by status. status={}, page={}, size={}",
                status,
                page,
                size
        );

        UUID sellerId = securityUtil.getCurrentUserId();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        Page<SellerOrderItem> orderItems = sellerOrderRepository.findBySeller_IdAndStatus(
                        sellerId,
                        status,
                        pageable
                );

        log.info(
                "Retrieved {} seller orders with status={} for sellerId={}",
                orderItems.getNumberOfElements(),
                status,
                sellerId
        );

        return PageResponse.<SellerOrderResponse>builder()
                .content(orderItems.getContent()
                        .stream()
                        .map(this::mapToSellerOrderResponse)
                        .toList())
                .page(orderItems.getNumber())
                .size(orderItems.getSize())
                .totalElements(orderItems.getTotalElements())
                .totalPages(orderItems.getTotalPages())
                .first(orderItems.isFirst())
                .last(orderItems.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public SellerOrderResponse getSellerOrder(UUID sellerOrderId) {
        UUID sellerId = securityUtil.getCurrentUserId();

        log.debug(
                "Fetching seller order details. sellerOrderId={}, sellerId={}",
                sellerOrderId,
                sellerId
        );

        SellerOrderItem sellerOrderItem =
                sellerOrderRepository
                        .findByIdAndSeller_Id(
                                sellerOrderId,
                                sellerId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Seller order not found"
                                )
                        );

        return mapToSellerOrderResponse(
                sellerOrderItem
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerOrderResponse> getPendingSellerOrders(
            int page,
            int size
    ) {
        UUID sellerId = securityUtil.getCurrentUserId();

        log.debug(
                "Fetching pending seller confirmations. sellerId={}, page={}, size={}",
                sellerId,
                page,
                size
        );

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.ASC,
                        "createdAt"
                )
        );

        Page<SellerOrderItem> orderItems =
                sellerOrderRepository.findBySeller_IdAndStatus(
                        sellerId,
                        FulfillmentStatus.PENDING_SELLER_CONFIRMATION,
                        pageable
                );

        log.info(
                "Retrieved pending seller confirmations. sellerId={}, count={}, total={}",
                sellerId,
                orderItems.getNumberOfElements(),
                orderItems.getTotalElements()
        );

        return PageResponse.<SellerOrderResponse>builder()
                .content(
                        orderItems.getContent()
                                .stream()
                                .map(this::mapToSellerOrderResponse)
                                .toList()
                )
                .page(orderItems.getNumber())
                .size(orderItems.getSize())
                .totalElements(orderItems.getTotalElements())
                .totalPages(orderItems.getTotalPages())
                .first(orderItems.isFirst())
                .last(orderItems.isLast())
                .build();
    }

    private SellerOrderResponse mapToSellerOrderResponse(SellerOrderItem item) {
        log.trace(
                "Mapping SellerOrderItem to SellerOrderResponse. sellerOrderId={}",
                item.getId()
        );

        SellerOrderResponse response = SellerOrderResponse.builder()
                .sellerOrderId(item.getId())
                .productId(item.getProductVariantId())
                .productName(item.getSku())
                .quantity(item.getQuantity())
                .orderStatus(item.getStatus())
                .paymentStatus(item.getPayment().getStatus())
                .orderId(item.getOrder().getId())
                .build();

        log.trace(
                "Mapped seller order response. sellerOrderId={}, status={}, paymentStatus={}",
                item.getId(),
                item.getStatus(),
                item.getPayment().getStatus()
        );

        return response;
    }


    @Transactional
    public SellerOrderResponse updateSellerOrderStatus(UUID sellerOrderId, SellerOrderUpdateRequest request) {
        log.debug(
                "Updating seller order status. sellerOrderId={}, requestedStatus={}",
                sellerOrderId,
                request.getStatus()
        );

        SellerOrderItem sellerOrderItem = sellerOrderRepository.findById(sellerOrderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Seller order item not found"
                                )
                        );

        securityUtil.validateCurrentUser(sellerOrderItem.getSeller().getEmail());

        FulfillmentStatus currentStatus = sellerOrderItem.getStatus();

        FulfillmentStatus requestedStatus = request.getStatus();

        if (!isValidTransition(currentStatus, requestedStatus)) {
            log.warn(
                    "Invalid seller fulfillment transition. sellerOrderItemId={}, {} -> {}",
                    sellerOrderId,
                    currentStatus,
                    requestedStatus
            );

            throw new IllegalArgsException(
                    "Invalid fulfillment transition: "
                            + currentStatus
                            + " -> "
                            + requestedStatus
            );
        }

        sellerOrderItem.setStatus(requestedStatus);
        SellerOrderItem saved = sellerOrderRepository.save(sellerOrderItem);

        log.info(
                "Seller fulfillment status updated. sellerOrderItemId={}, {} -> {}",
                sellerOrderId,
                currentStatus,
                requestedStatus
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

        AuditRequest sellerAudit = new AuditRequest(
                EntityType.SELLER_ORDER_ITEM,
                saved.getId(),
                currentStatus.name(),
                requestedStatus.name(),
                "FULFILLMENT_STATUS_CHANGED",
                currentUserId,
                currentUserRole,
                "Seller order item status updated from seller"
        );
        auditService.recordStatusChange(sellerAudit);

        //update order item status as well
        OrderItem orderItem = sellerOrderItem.getOrderItem();
        FulfillmentStatus currentOrderItemStatus = orderItem.getStatus();
        orderItem.setStatus(requestedStatus);
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);

        AuditRequest orderItemAudit = new AuditRequest(
                EntityType.ORDER_ITEM,
                savedOrderItem.getId(),
                currentOrderItemStatus.name(),
                requestedStatus.name(),
                "FULFILLMENT_STATUS_CHANGED",
                null,
                "SYSTEM",
                "Order item status changed after seller updates seller order item"
        );
        auditService.recordStatusChange(orderItemAudit);

        return mapToSellerOrderResponse(saved);
    }


    private boolean isValidTransition(FulfillmentStatus current, FulfillmentStatus next) {
        if (current == null) {
            return next == FulfillmentStatus.PENDING_SELLER_CONFIRMATION;
        }

        return switch (current) {
            case PENDING_SELLER_CONFIRMATION ->
                    next == FulfillmentStatus.CONFIRMED;

            case CONFIRMED ->
                    next == FulfillmentStatus.PROCESSING;

            case PROCESSING ->
                    next == FulfillmentStatus.READY_FOR_PICKUP;

            case READY_FOR_PICKUP,
                 SHIPPED,
                 IN_TRANSIT,
                 OUT_FOR_DELIVERY,
                 DELIVERED,
                 RETURN_REQUESTED,
                 RETURN_APPROVED,
                 RETURN_PICKED_UP,
                 RETURN_COMPLETED,
                 RETURN_REJECTED,
                 CANCELLED ->
                    false;
        };
    }
}