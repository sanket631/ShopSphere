package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.PaymentResponse;
import com.backend.ShopSphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/{orderId}")
    public ResponseEntity <?> processPayment(@PathVariable UUID orderId){
        log.info("=== PROCESS PAYMENT API CALLED ===");
        log.debug("Processing payment for orderId: {}", orderId);

        PaymentResponse result = paymentService.processPayment(orderId);

        log.debug("Payment processing result: {}", result);

        return ResponseEntity.ok(result);
    }
}
