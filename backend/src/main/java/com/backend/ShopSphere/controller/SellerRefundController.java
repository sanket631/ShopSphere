package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.PageResponse;
import com.backend.ShopSphere.dto.RefundResponse;
import com.backend.ShopSphere.dto.RefundStatusUpdateRequest;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/seller/refunds")
@RequiredArgsConstructor
@Slf4j
public class SellerRefundController {

    private final RefundService refundService;

    @GetMapping
    public ResponseEntity<PageResponse<RefundResponse>> getAllRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePagination(page, size);

        return ResponseEntity.ok(
                refundService.getSellerRefunds(page, size)
        );
    }

    @GetMapping("/pending")
    public ResponseEntity<PageResponse<RefundResponse>> getPendingRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePagination(page, size);

        return ResponseEntity.ok(
                refundService.getPendingSellerRefunds(page, size)
        );
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponse> getRefund(
            @PathVariable UUID refundId
    ) {
        return ResponseEntity.ok(
                refundService.getSellerRefund(refundId)
        );
    }

    @PatchMapping("/{refundId}/status")
    public ResponseEntity<RefundResponse> updateRefundStatus(
            @PathVariable UUID refundId,
            @Valid @RequestBody RefundStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                refundService.updateRefundStatus(
                        refundId,
                        request
                )
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgsException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgsException(
                    "Size must be between 1 and 100"
            );
        }
    }
}