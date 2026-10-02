package com.backend.ShopSphere.listener;

import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.enums.EventConsumers;
import com.backend.ShopSphere.enums.RefundStatus;
import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.events.OrderItemCancelledEvent;
import com.backend.ShopSphere.events.RefundStatusChangedEvent;
import com.backend.ShopSphere.repository.OrderRepository;
import com.backend.ShopSphere.repository.ProductVariantRepository;
import com.backend.ShopSphere.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener implements EventListener {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public void process(BaseEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

//        switch (event.getEventType()) {
//            case PAYMENT_SUCCESS -> handlePaymentSuccess(order);
//
//            case PAYMENT_FAILURE -> handlePaymentFailure(order);
//
//            case ORDER_ITEM_CANCELLED -> handleOrderItemCancelled(
//                    order,
//                    (OrderItemCancelledEvent) event
//            );
//
//            case REFUND_STATUS_CHANGED -> handleRefundStatusChanged(
//                    order,
//                    (RefundStatusChangedEvent) event
//            );
//
//            case SHIPMENT_STATUS_CHANGED ->
//                    log.debug(
//                            "Shipment notification handled by ShipmentEventListener for order: {}",
//                            order.getId()
//                    );
//
//            default ->
//                    log.warn(
//                            "Unhandled event type for notification: {}",
//                            event.getEventType()
//                    );
//        }
    }

    private void handlePaymentSuccess(Order order) {
        String customerEmail = order.getUser().getEmail();

        notificationService.sendEmail(
                customerEmail,
                "Payment Successful",
                "Your payment for order " + order.getId() + " was successful"
        );

        log.info(
                "Payment success notification sent to customer for order: {}",
                order.getId()
        );

        for (OrderItem item : order.getOrderItems()) {
            ProductVariant productVariant = productVariantRepository
                    .findById(item.getProductVariantId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            notificationService.sendEmail(
                    productVariant.getProduct().getSeller().getEmail(),
                    "New Order Received",
                    "A new order has been assigned. Product details:"
                            + orderItemDetails(item)
            );
        }

        log.info(
                "Seller notifications sent for order: {}",
                order.getId()
        );
    }

    private void handlePaymentFailure(Order order) {
        String customerEmail = order.getUser().getEmail();

        notificationService.sendEmail(
                customerEmail,
                "Payment Failed",
                "Your payment for order " + order.getId()
                        + " failed. Please try again."
        );

        log.info(
                "Payment failure notification sent for order: {}",
                order.getId()
        );
    }

    private void handleOrderItemCancelled(
            Order order,
            OrderItemCancelledEvent event
    ) {
        String customerEmail = order.getUser().getEmail();

        notificationService.sendEmail(
                customerEmail,
                "Order Item Cancelled",
                "An item in your order " + order.getId()
                        + " has been cancelled."
        );

        log.info(
                "Cancellation notification sent for order item {}",
                event.getOrderItemId()
        );
    }

    private void handleRefundStatusChanged(
            Order order,
            RefundStatusChangedEvent event
    ) {
        String customerEmail = order.getUser().getEmail();

        RefundStatus status = event.getStatus();

        switch (status) {
            case APPROVED -> notificationService.sendEmail(
                    customerEmail,
                    "Refund Approved",
                    "Your refund of " + event.getAmount()
                            + " for order " + event.getOrderId()
                            + " has been approved."
            );

            case PROCESSING -> notificationService.sendEmail(
                    customerEmail,
                    "Refund Processing",
                    "Your refund of " + event.getAmount()
                            + " for order " + event.getOrderId()
                            + " is being processed."
            );

            case REFUNDED -> notificationService.sendEmail(
                    customerEmail,
                    "Refund Successful",
                    "Your refund of " + event.getAmount()
                            + " for order " + event.getOrderId()
                            + " has been successfully processed."
            );

            case FAILED -> notificationService.sendEmail(
                    customerEmail,
                    "Refund Failed",
                    "Your refund of " + event.getAmount()
                            + " for order " + event.getOrderId()
                            + " could not be processed. We will retry."
            );

            case REJECTED -> notificationService.sendEmail(
                    customerEmail,
                    "Refund Rejected",
                    "Your refund request for order " + event.getOrderId()
                            + " has been rejected."
            );

            default -> log.debug(
                    "No customer notification required for refund status {}",
                    status
            );
        }

        log.info(
                "Refund status notification sent for refund {} with status {}",
                event.getRefundId(),
                status
        );
    }

    private String orderItemDetails(OrderItem item) {
        return "\nProduct Id: " + item.getProductVariantId()
                + "\nSKU: " + item.getSku()
                + "\nQuantity: " + item.getQuantity()
                + "\nSubtotal: " + item.getSubtotal()
                + "\nPayment Status: SUCCESS";
    }

    @Override
    public String getConsumerName() {
        return EventConsumers.NOTIFICATION_PROCESSOR;
    }
}