package com.backend.ShopSphere.listener;
import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.entity.Payment;
import com.backend.ShopSphere.entity.Refund;
import com.backend.ShopSphere.enums.EventConsumers;
import com.backend.ShopSphere.enums.PaymentStatus;
import com.backend.ShopSphere.enums.RefundStatus;
import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.events.OrderItemCancelledEvent;
import com.backend.ShopSphere.events.RefundStatusChangedEvent;
import com.backend.ShopSphere.repository.OrderItemRepository;
import com.backend.ShopSphere.repository.OrderRepository;
import com.backend.ShopSphere.repository.RefundRepository;
import com.backend.ShopSphere.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefundListener implements EventListener {

    private final RefundRepository refundRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final RefundService refundService;

    @Override
    public void process(BaseEvent event) {

        switch (event.getEventType()) {

            case ORDER_ITEM_CANCELLED -> handleCancellation(
                    (OrderItemCancelledEvent) event
            );

            case REFUND_STATUS_CHANGED -> handleStatusChange(
                    (RefundStatusChangedEvent) event
            );

            default -> log.warn(
                    "Unhandled event type for refund processor: {}",
                    event.getEventType()
            );
        }
    }

    private void handleCancellation(OrderItemCancelledEvent event) {

        UUID orderItemId = event.getOrderItemId();

        if (refundRepository.existsByOrderItemId(orderItemId)) {
            log.info(
                    "Refund already exists. orderItemId={}, eventId={}",
                    orderItemId,
                    event.getEventId()
            );
            return;
        }

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() ->
                        new RuntimeException("Order item not found"));

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        Payment payment = order.getPayment();

        if (payment == null) {
            throw new IllegalStateException(
                    "Payment not found for order " + order.getId()
            );
        }

        if (PaymentStatus.SUCCESS.equals(payment.getStatus())){
            Refund refund = Refund.builder()
                    .orderItem(orderItem)
                    .order(order)
                    .status(RefundStatus.PENDING_SELLER_APPROVAL)
                    .amount(event.getRefundAmount())
                    .requestedAt(LocalDateTime.now())
                    .build();

            Refund saved = refundRepository.save(refund);

            log.info(
                    "Refund created. refundId={}, orderId={}, orderItemId={}, amount={}",
                    saved.getId(),
                    order.getId(),
                    orderItem.getId(),
                    saved.getAmount()
            );
        }

        else {
            log.error("Current payment status: {}. Not eligible for refund", payment.getStatus());
        }
    }

    private void handleStatusChange(RefundStatusChangedEvent event) {

        if (!RefundStatus.APPROVED.equals(event.getStatus())) {
            log.info(
                    "Ignoring refund status {} for refund {}",
                    event.getStatus(),
                    event.getRefundId()
            );
            return;
        }

        log.info(
                "Refund {} approved. Starting refund processing",
                event.getRefundId()
        );

        refundService.processRefund(event.getRefundId());
    }

    @Override
    public String getConsumerName() {
        return EventConsumers.REFUND_PROCESSOR;
    }
}