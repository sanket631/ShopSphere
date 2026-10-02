package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.PaymentGatewayResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {
    PaymentGatewayResponse processPayment(
            UUID orderId,
            BigDecimal amount
    );

    PaymentGatewayResponse refundPayment(
            String transactionId,
            BigDecimal amount
    );
}
