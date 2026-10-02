package com.backend.ShopSphere.listener;

import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.entity.SellerOrderItem;
import com.backend.ShopSphere.enums.EventConsumers;
import com.backend.ShopSphere.enums.EventType;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import com.backend.ShopSphere.events.PaymentSuccessEvent;
import com.backend.ShopSphere.repository.OrderItemRepository;
import com.backend.ShopSphere.repository.OrderRepository;
import com.backend.ShopSphere.repository.ProductVariantRepository;
import com.backend.ShopSphere.repository.SellerOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SellerFulfillmentListener implements EventListener {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SellerOrderRepository sellerOrderRepository;

    @Transactional
    @Override
    public void process(BaseEvent event) {
        // Only handle PAYMENT_SUCCESS events
        if (event.getEventType() != EventType.PAYMENT_SUCCESS) {
            return;
        }

        PaymentSuccessEvent paymentEvent = (PaymentSuccessEvent) event;
        log.info("Creating seller fulfillment for order: {}", paymentEvent.getOrderId());

        Order order = orderRepository.findById(paymentEvent.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        int sellerItemCount = 0;
        List<OrderItem> orderItemList = orderItemRepository.findByOrderId(paymentEvent.getOrderId());
        for (OrderItem item : orderItemList) {
            ProductVariant product = productVariantRepository
                    .findById(item.getProductVariantId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            SellerOrderItem sellerOrderItem = SellerOrderItem.builder()
                    .seller(product.getProduct().getSeller())
                    .order(order)
                    .orderItem(item)
                    .productVariantId(item.getProductVariantId())
                    .sku(item.getSku())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .status(FulfillmentStatus.PENDING_SELLER_CONFIRMATION)
                    .payment(order.getPayment())
                    .build();

            sellerOrderRepository.save(sellerOrderItem);
            sellerItemCount++;
        }

        log.info("Seller order items created - orderId={}, totalItems={}",
                order.getId(), sellerItemCount);
    }

    @Override
    public String getConsumerName() {
        return EventConsumers.SELLER_FULFILLMENT;
    }
}