package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.PageResponse;
import com.backend.ShopSphere.dto.SellerOrderResponse;
import com.backend.ShopSphere.dto.SellerOrderUpdateRequest;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.service.SellerOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/api/seller/order")
@RequiredArgsConstructor
public class SellerOrderController {
    private final SellerOrderService sellerOrderService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        log.info("=== GET ALL SELLER ORDERS API CALLED ===");
        log.debug("Fetching all orders for current seller");

        validatePagination(page, size);

        PageResponse<SellerOrderResponse> response = sellerOrderService.getAllOrderProducts(page, size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<PageResponse<SellerOrderResponse>> getOrdersByStatus(
            @PathVariable FulfillmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.debug(
                "GET /api/seller/orders/status/{}?page={}&size={}",
                status,
                page,
                size
        );
        validatePagination(page, size);

        return ResponseEntity.ok(
                sellerOrderService.getOrdersByStatus(
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{sellerOrderId}")
    public ResponseEntity<SellerOrderResponse> getSellerOrder(
            @PathVariable UUID sellerOrderId
    ) {
        return ResponseEntity.ok(
                sellerOrderService.getSellerOrder(
                        sellerOrderId
                )
        );
    }

    @GetMapping("/pending")
    public ResponseEntity<PageResponse<SellerOrderResponse>> getPendingOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePagination(page, size);

        return ResponseEntity.ok(
                sellerOrderService.getPendingSellerOrders(
                        page,
                        size
                )
        );
    }

    @PatchMapping("/{sellerOrderId}")
    public ResponseEntity<?> updateSellerOrder(
            @PathVariable UUID sellerOrderId,
            @Valid @RequestBody SellerOrderUpdateRequest request){

        log.info("=== UPDATE SELLER ORDER STATUS API CALLED ===");
        log.debug("Update seller order request - sellerOrderId: {}, newStatus: {}",
                sellerOrderId, request.getStatus());

        SellerOrderResponse response = sellerOrderService.updateSellerOrderStatus(sellerOrderId, request);

        log.info("Seller order status updated successfully - sellerOrderId: {}, productName: {}, oldStatus: {}, newStatus: {}",
                sellerOrderId, response.getProductName(), response.getOrderStatus(), request.getStatus());
        log.debug("Updated seller order details: {}", response);

        return ResponseEntity.ok(response);
    }

    private void validatePagination(int page, int size){
        if (page < 0) {
            throw new IllegalArgsException("Page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgsException("Size must be between 1 and 100");
        }
    }
}