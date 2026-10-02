package com.backend.ShopSphere.listener;

import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.entity.Cart;
import com.backend.ShopSphere.enums.EventConsumers;
import com.backend.ShopSphere.enums.EventType;
import com.backend.ShopSphere.events.PaymentSuccessEvent;
import com.backend.ShopSphere.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class CartCleanupListener implements EventListener {

    private final CartRepository cartRepository;

    @Transactional
    @Override
    public void process(BaseEvent event) {
        // Only handle PAYMENT_SUCCESS events
        if (event.getEventType() != EventType.PAYMENT_SUCCESS) {
            log.debug("Skipping non-PAYMENT_SUCCESS event: {}", event.getEventType());
            return;
        }

        PaymentSuccessEvent paymentEvent = (PaymentSuccessEvent) event;
        UUID userId = paymentEvent.getUserId();
        UUID orderId = paymentEvent.getOrderId();

        log.info("Cleaning cart for user: {}, order: {}", userId, orderId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Cart not found for user: {}", userId);
                    return new RuntimeException("Cart not found for user: " + userId);
                });

        int cartSize = cart.getCartItems() != null ? cart.getCartItems().size() : 0;

        if (cartSize == 0) {
            log.info("Cart already empty for user: {}", userId);
            return;
        }

        // Clear the cart
        cart.getCartItems().clear();

        log.info("Cart cleared for user={}, removedItems={}", userId, cartSize);
    }

    @Override
    public String getConsumerName() {
        return EventConsumers.CART_CLEANUP;
    }
}