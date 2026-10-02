package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.CouponCreateRequest;
import com.backend.ShopSphere.dto.CouponResponse;
import com.backend.ShopSphere.dto.CouponUpdateRequest;
import com.backend.ShopSphere.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
@Slf4j
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(
            @Valid @RequestBody CouponCreateRequest request){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(couponService.createCoupon(request));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse> getCoupon(
            @PathVariable UUID couponId){

        return ResponseEntity.ok(couponService.getCoupon(couponId));
    }

    @GetMapping
    public ResponseEntity<Page<CouponResponse>> getCoupons(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size){

        return ResponseEntity.ok(couponService.getCoupons(page,size));
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable UUID couponId,
            @Valid @RequestBody CouponUpdateRequest request){

        return ResponseEntity.ok(couponService.updateCoupon(couponId,request));
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(
            @PathVariable UUID couponId){

        couponService.deleteCoupon(couponId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{couponId}/activate")
    public ResponseEntity<Void> activateCoupon(
            @PathVariable UUID couponId){

        couponService.activateCoupon(couponId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{couponId}/deactivate")
    public ResponseEntity<Void> deactivateCoupon(
            @PathVariable UUID couponId){

        couponService.deactivateCoupon(couponId);

        return ResponseEntity.ok().build();
    }
}