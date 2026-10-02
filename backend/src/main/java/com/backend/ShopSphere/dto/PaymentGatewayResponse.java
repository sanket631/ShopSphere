package com.backend.ShopSphere.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentGatewayResponse {
    private boolean successful;
    private String transactionId;
    private String message;
}
