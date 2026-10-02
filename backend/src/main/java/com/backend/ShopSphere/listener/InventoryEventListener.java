package com.backend.ShopSphere.listener;

import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.enums.ReturnStatus;
import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.enums.EventConsumers;
import com.backend.ShopSphere.enums.PaymentStatus;
import com.backend.ShopSphere.events.OrderItemCancelledEvent;
import com.backend.ShopSphere.events.ReturnStatusChangedEvent;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.OrderItemRepository;
import com.backend.ShopSphere.repository.OrderRepository;
import com.backend.ShopSphere.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryEventListener implements EventListener {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;

    @Override
    public void process(BaseEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        switch (event.getEventType()) {
            case PAYMENT_SUCCESS -> {
                log.info("Inventory confirmation for order: {}", order.getId());
                inventoryService.confirmStock(order);
            }

            case PAYMENT_FAILURE -> {
                log.info("Inventory release for order: {}", order.getId());
                inventoryService.releaseStock(order);
            }

            case RESERVATION_EXPIRED -> {
                log.info("Inventory release for expired reservation: {}", order.getId());
                inventoryService.releaseStock(order);
            }

            case ORDER_ITEM_CANCELLED -> {
                OrderItemCancelledEvent cancelledEvent = (OrderItemCancelledEvent) event;
                OrderItem orderItem = orderItemRepository.findById(cancelledEvent.getOrderItemId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Order item not found")
                        );
                PaymentStatus paymentStatus = order.getPayment().getStatus();

                if (PaymentStatus.PENDING.equals(paymentStatus)) {
                    inventoryService.releaseStock(orderItem);
                } else if (PaymentStatus.SUCCESS.equals(paymentStatus)) {
                    inventoryService.restoreStock(orderItem);
                } else {
                    log.warn("No inventory action for cancelled order: {}, paymentStatus={}",
                            order.getId(), paymentStatus);
                }
            }

            case RETURN_STATUS_CHANGED -> {
                ReturnStatusChangedEvent returnEvent = (ReturnStatusChangedEvent) event;
                if(!ReturnStatus.COMPLETED.equals(((ReturnStatusChangedEvent) event).getStatus())){
                    return;
                }

                OrderItem orderItem = orderItemRepository.findById(returnEvent.getOrderItemId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Order item not found")
                        );
                inventoryService.restoreStock(orderItem);
            }

            default -> log.warn("Unhandled event type for inventory: {}", event.getEventType());
        }

        log.info("Inventory processing completed for order: {}", order.getId());
    }

    @Override
    public String getConsumerName() {
        return EventConsumers.INVENTORY_EVENT_PROCESSOR;
    }
}