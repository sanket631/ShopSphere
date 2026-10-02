package com.backend.ShopSphere.listener;

import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.entity.Coupon;
import com.backend.ShopSphere.entity.CouponUsage;
import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.enums.EventConsumers;
import com.backend.ShopSphere.enums.EventType;
import com.backend.ShopSphere.events.PaymentSuccessEvent;
import com.backend.ShopSphere.repository.CouponRepository;
import com.backend.ShopSphere.repository.CouponUsageRepository;
import com.backend.ShopSphere.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponListener implements EventListener {

    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    @Override
    public void process(BaseEvent event) {
        // Only handle PAYMENT_SUCCESS events
        if (event.getEventType() != EventType.PAYMENT_SUCCESS) {
            return;
        }

        PaymentSuccessEvent paymentEvent = (PaymentSuccessEvent) event;
        log.info("Consuming coupon for order: {}", paymentEvent.getOrderId());

        Order order = orderRepository.findById(paymentEvent.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Coupon orderCoupon = order.getCoupon();

        if (orderCoupon == null) {
            log.debug("No coupon applied to order: {}", paymentEvent.getOrderId());
            return;
        }

        // Save coupon usage
        Coupon coupon = couponRepository
                .findByIdForUpdate(orderCoupon.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Coupon not found: " + orderCoupon.getId()
                        )
                );

        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .user(order.getUser())
                .order(order)
                .discountAmount(order.getDiscountAmount())
                .build();

        couponUsageRepository.save(usage);

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        log.info("Coupon {} consumed for order {}", coupon.getCode(), order.getId());
    }

    @Override
    public String getConsumerName() {
        return EventConsumers.COUPON_CONSUMPTION;
    }
}