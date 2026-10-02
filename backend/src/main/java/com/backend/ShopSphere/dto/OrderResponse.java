package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.OrderStatus;
import com.backend.ShopSphere.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
public class OrderResponse {
    private UUID orderId;

    private BigDecimal totalAmount;

    private OrderStatus status;

    private PaymentStatus paymentStatus;

    private BigDecimal discountAmount;

    private BigDecimal shippingCharge;

    private BigDecimal otherCharges;

    private BigDecimal finalAmount;

    private String couponCode;

    private List<OrderItemResponse> items;

    private ShippingAddressResponse shippingAddress;
}
