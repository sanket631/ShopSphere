package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.PaymentGatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayResponse processPayment(
            UUID orderId,
            BigDecimal amount
    ) {
        String transactionId = "MOCK-" + UUID.randomUUID();

        log.info(
                "Mock payment processed. orderId={}, amount={}, transactionId={}",
                orderId,
                amount,
                transactionId
        );

        return PaymentGatewayResponse.builder()
                .successful(true)
                .transactionId(transactionId)
                .message("Payment successful")
                .build();
    }

    @Override
    public PaymentGatewayResponse refundPayment(
            String transactionId,
            BigDecimal amount
    ) {
        String refundTransactionId = "MOCK-REFUND-" + UUID.randomUUID();

        log.info(
                "Mock refund processed. transactionId={}, amount={}, refundTransactionId={}",
                transactionId,
                amount,
                refundTransactionId
        );

        return PaymentGatewayResponse.builder()
                .successful(true)
                .transactionId(refundTransactionId)
                .message("Refund successful")
                .build();
    }
}