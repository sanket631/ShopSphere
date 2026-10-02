package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.AddToCartRequest;
import com.backend.ShopSphere.dto.CartResponse;
import com.backend.ShopSphere.dto.CartItemUpdateRequest;
import com.backend.ShopSphere.service.CartService;
import com.backend.ShopSphere.service.ShoppingFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@Slf4j
public class CartController {
    private final CartService cartService;
    private final ShoppingFacade shoppingFacade;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@Valid @RequestBody AddToCartRequest request){
        log.info("=== ADD TO CART API CALLED ===");
        log.debug("Add to cart request received - productId: {}, quantity: {}",
                request.getProductId(), request.getQuantity());

        CartResponse response = cartService.addToCart(request);

        log.info("Item added to cart successfully - cartId: {}, totalProductsInCart: {}",
                response.getCartId(),
                response.getProducts() != null ? response.getProducts().size() : 0);
        log.debug("Add to cart response: {}", response);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getCart(){
        log.info("=== GET CART API CALLED ===");
        log.debug("Fetching cart details for current user");

        CartResponse response = cartService.getCart();

        int itemCount = response.getProducts() != null ? response.getProducts().size() : 0;

        if(itemCount == 0) {
            log.info("Cart is empty for current user");
        } else {
            log.info("Cart retrieved successfully - cartId: {}, totalItems: {}",
                    response.getCartId(), itemCount);

            log.debug("Cart items summary:");
            response.getProducts().forEach(product ->
                    log.debug("  - Product: {}, Quantity: {}, Price: {}",
                            product.getName(), product.getQuantity(), product.getPrice())
            );
        }

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/item/{cartItemId}")
    public ResponseEntity<?> updateCartItem(@PathVariable UUID cartItemId, @RequestBody CartItemUpdateRequest request){
        log.info("=== UPDATE CART ITEM API CALLED ===");
        log.debug("Update cart item request - cartItemId: {}, newQuantity: {}", cartItemId, request.getQuantity());

        CartResponse response = cartService.updateCartItem(cartItemId, request);

        log.info("Cart item updated successfully - cartItemId: {}, newQuantity: {}",
                cartItemId, request.getQuantity());
        log.debug("Updated cart details: {}", response);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable UUID cartItemId){
        log.info("=== REMOVE CART ITEM API CALLED ===");
        log.debug("Remove cart item request - cartItemId: {}", cartItemId);

        cartService.removeCartItem(cartItemId);

        log.info("Cart item removed successfully - cartItemId: {}", cartItemId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{cartItemId}/move-to-wishlist")
    public ResponseEntity<?> moveToWishList(@PathVariable UUID cartItemId){
        shoppingFacade.moveCartToWishlist(cartItemId);
        return ResponseEntity.ok().build();
    }
}