package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.FulfillmentStatus;
import com.backend.ShopSphere.enums.PaymentStatus;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerOrderResponse {
    private UUID sellerOrderId;
    private UUID productId;
    private String productName;
    private Integer quantity;
    private UUID orderId;
    private FulfillmentStatus orderStatus;
    private PaymentStatus paymentStatus;
}
