package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.service.ShoppingFacade;
import com.backend.ShopSphere.service.WishListItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishListController {
    private final WishListItemService wishListItemService;
    private final ShoppingFacade shoppingFacade;

    @PostMapping("/{productId}")
    public ResponseEntity<?> addToWishList(@PathVariable UUID productId){
        wishListItemService.addWishListItem(productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<?> getWishList(){
        return ResponseEntity.ok(wishListItemService.getWishList());
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFromWishList(@PathVariable UUID productId){
        wishListItemService.removeWishListItem(productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/move-to-cart")
    public ResponseEntity<?> moveToCart(@PathVariable UUID productId){
        return ResponseEntity.ok(shoppingFacade.moveWishlistToCart(productId));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<?> isWishListed(@PathVariable UUID productId){
        boolean wishListed = wishListItemService.isWishListed(productId);
        return ResponseEntity.ok(Map.of("wishlisted", wishListed));
    }
}
