package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemUpdateRequest {

    @Positive
    private Integer quantity;

}
