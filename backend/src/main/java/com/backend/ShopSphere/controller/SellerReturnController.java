package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.PageResponse;
import com.backend.ShopSphere.dto.ReturnStatusUpdateRequest;
import com.backend.ShopSphere.dto.SellerReturnResponse;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.service.SellerReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/seller/return")
@RequiredArgsConstructor
public class SellerReturnController {

    private final SellerReturnService sellerReturnService;

    @GetMapping
    public ResponseEntity<PageResponse<SellerReturnResponse>> getSellerReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePagination(page, size);
        return ResponseEntity.ok(
                sellerReturnService.getAllReturns(page, size)
        );
    }

    @GetMapping("/pending")
    public ResponseEntity<PageResponse<SellerReturnResponse>> getPendingReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePagination(page, size);
        return ResponseEntity.ok(
                sellerReturnService.getPendingReturns(page, size)
        );
    }

    @GetMapping("/{returnId}")
    public ResponseEntity<SellerReturnResponse> getReturn(
            @PathVariable UUID returnId
    ) {
        return ResponseEntity.ok(
                sellerReturnService.getReturn(returnId)
        );
    }

    @PatchMapping("/{returnId}/status")
    public ResponseEntity<SellerReturnResponse> updateReturnStatus(
            @PathVariable UUID returnId,
            @Valid @RequestBody ReturnStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                sellerReturnService.updateReturnStatus(returnId, request)
        );
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
