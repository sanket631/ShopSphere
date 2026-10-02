package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.BuyNowRequest;
import com.backend.ShopSphere.dto.OrderResponse;
import com.backend.ShopSphere.service.ShoppingFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shopping")
public class ShoppingController {
    private final ShoppingFacade shoppingFacade;

    @PostMapping("/buy-now")
    public ResponseEntity<?> buyNow(@Valid @RequestBody BuyNowRequest request){
        OrderResponse response = shoppingFacade.buyNow(request);
        return ResponseEntity.ok(response);
    }
}
