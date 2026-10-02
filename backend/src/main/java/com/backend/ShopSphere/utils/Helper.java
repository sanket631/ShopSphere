package com.backend.ShopSphere.utils;

import com.backend.ShopSphere.entity.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class Helper {
    public BigDecimal calculateRefundAmount(OrderItem orderItem) {
        if (orderItem.getSubtotal() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal itemDiscount = orderItem.getDiscountAmount() != null
                ? orderItem.getDiscountAmount()
                : BigDecimal.ZERO;

        return orderItem.getSubtotal()
                .subtract(itemDiscount)
                .max(BigDecimal.ZERO);
    }
}
