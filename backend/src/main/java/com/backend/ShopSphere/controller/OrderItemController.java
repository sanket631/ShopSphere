package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.CancelOrderItemRequest;
import com.backend.ShopSphere.dto.CancelOrderItemResponse;
import com.backend.ShopSphere.dto.ReturnRequest;
import com.backend.ShopSphere.dto.ReturnResponse;
import com.backend.ShopSphere.service.OrderItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/order-items")
public class OrderItemController {
    private final OrderItemService orderItemService;

    @PostMapping("/{orderItemId}/cancel")
    public ResponseEntity<CancelOrderItemResponse> cancelOrderItem(
            @PathVariable UUID orderItemId,
            @Valid @RequestBody CancelOrderItemRequest request) {

        return ResponseEntity.ok(
                orderItemService.cancelOrderItem(orderItemId, request)
        );
    }

    @PostMapping("/{orderItemId}/return")
    public ResponseEntity<ReturnResponse> returnOrderItem(
            @PathVariable UUID orderItemId,
            @Valid @RequestBody ReturnRequest request) {

        return ResponseEntity.ok(orderItemService.requestReturn(orderItemId, request));
    }
}
