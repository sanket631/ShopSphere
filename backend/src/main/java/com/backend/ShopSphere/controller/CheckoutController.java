package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.CheckoutOrderRequest;
import com.backend.ShopSphere.dto.OrderResponse;
import com.backend.ShopSphere.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/orders")
public class CheckoutController {
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutOrderRequest request){
        return ResponseEntity.ok(orderService.checkoutCart(request));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getMyOrders(){
        log.info("=== GET MY ORDERS API CALLED ===");
        log.debug("Fetching all orders for current user");

        List<OrderResponse> response = orderService.getAllOrders();

        int orderCount = response.size();

        if(orderCount == 0) {
            log.info("No orders found for current user");
        } else {
            log.info("Orders retrieved successfully - totalOrders: {}", orderCount);

            log.debug("Orders summary:");
            response.forEach(order ->
                    log.debug("  - OrderId: {}, TotalAmount: {}, Status: {}, CreatedAt: {}",
                            order.getOrderId(), order.getTotalAmount(), order.getStatus(), order.getPaymentStatus())
            );
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/one/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable UUID orderId){
        log.info("=== GET ORDER BY ID API CALLED ===");
        log.debug("Fetching order details - orderId: {}", orderId);

        OrderResponse response = orderService.getOrder(orderId);

        log.info("Order retrieved successfully - orderId: {}, totalAmount: {}, orderStatus: {}, paymentStatus: {}",
                response.getOrderId(), response.getTotalAmount(), response.getStatus(), response.getPaymentStatus());
        log.debug("Order details: {}", response);

        return ResponseEntity.ok(response);
    }
}
