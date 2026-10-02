package com.backend.ShopSphere.service;

import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.enums.OrderStatus;
import com.backend.ShopSphere.exception.IllegalArgsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class OrderLifecycleService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING_CONFIRMATION,
            Set.of(OrderStatus.CONFIRMED, OrderStatus.RESERVATION_EXPIRED),

            OrderStatus.CONFIRMED,
            Set.of()
    );

    public void transition(Order order, OrderStatus targetStatus) {
        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == targetStatus) {
            throw new IllegalArgsException(
                    "Order is already " + targetStatus
            );
        }

        Set<OrderStatus> allowedStatuses =
                ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowedStatuses.contains(targetStatus)) {
            log.warn(
                    "Invalid order status transition. orderId={}, currentStatus={}, targetStatus={}",
                    order.getId(), currentStatus, targetStatus
            );

            throw new IllegalArgsException(
                    "Invalid order status transition from "
                            + currentStatus + " to " + targetStatus
            );
        }

        order.setStatus(targetStatus);

        log.info(
                "Order status transition. orderId={}, from={}, to={}",
                order.getId(), currentStatus, targetStatus
        );
    }
}
