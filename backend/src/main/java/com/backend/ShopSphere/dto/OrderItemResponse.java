package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.FulfillmentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class OrderItemResponse {

    private UUID orderItemId;

    private UUID productId;

    private String productName;

    private BigDecimal productPrice;

    private Integer quantity;

    private BigDecimal subtotal;

    private FulfillmentStatus status;

}
