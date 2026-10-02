package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.ShipmentStatus;
import com.backend.ShopSphere.enums.ShipmentType;
import com.backend.ShopSphere.enums.ShippingMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ShipmentResponse {
    private UUID shipmentId;
    private ShipmentType type;
    private UUID orderId;
    private UUID sellerId;
    private UUID sellerOrderItemId;
    private UUID productVariantId;
    private String sku;
    private Integer quantity;
    private String trackingNumber;
    private ShippingMethod shippingMethod;
    private BigDecimal shippingCharge;
    private String carrier;
    private ShipmentStatus status;
    private LocalDate estimatedDeliveryDate;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private ShippingAddressResponse shippingAddress;
}
