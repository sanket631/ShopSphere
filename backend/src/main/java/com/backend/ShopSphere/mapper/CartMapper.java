package com.backend.ShopSphere.mapper;

import com.backend.ShopSphere.dto.CartProductResponse;
import com.backend.ShopSphere.dto.CartResponse;
import com.backend.ShopSphere.entity.Cart;
import com.backend.ShopSphere.entity.CartItem;
import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartMapper {

    private final CartItemRepository cartItemRepository;

    private CartProductResponse mapToCartProductResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        return CartProductResponse.builder()
                .cartItemId(item.getId())
                .productId(variant.getProduct().getId()) // Get Product ID
                .variantId(variant.getId()) // Add variant ID
                .name(variant.getProduct().getName()) // Get actual product name
                .sku(variant.getSku())
                .price(variant.getSellingPrice())
                .quantity(item.getQuantity())
                .totalPrice(variant.getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    public CartResponse mapToCartResponse(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);

        List<CartProductResponse> productItems = cartItems.stream()
                .map(this::mapToCartProductResponse)
                .toList();

        // Calculate total amount
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProductVariant().getSellingPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .products(productItems)
                .totalAmount(totalAmount)
                .totalItems(cartItems.stream().mapToInt(CartItem::getQuantity).sum())
                .build();
    }
}
