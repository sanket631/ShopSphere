package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.AddToCartRequest;
import com.backend.ShopSphere.dto.CartProductResponse;
import com.backend.ShopSphere.dto.CartResponse;
import com.backend.ShopSphere.dto.CartItemUpdateRequest;
import com.backend.ShopSphere.entity.*;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.mapper.CartMapper;
import com.backend.ShopSphere.repository.*;
import com.backend.ShopSphere.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class CartService {
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final ProductVariantRepository productVariantRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        log.debug("Request received to add to cart: {}", request.toString());
        String currentUserEmail = securityUtil.getCurrentUserEmail();
        log.info("Extracted user email: {}", currentUserEmail);

        User user = userRepository.findByEmail(currentUserEmail).orElseThrow(
                () -> {
                    log.error("User not found during add to cart api");
                    throw new ResourceNotFoundException("User not found");
                }
        );
        Cart cart = cartRepository.findByUser(user).orElseGet(
                () -> {
                    log.info("No cart found for the user. Creating a new cart for the user");
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                }
        );

        ProductVariant product = productVariantRepository.findById(request.getProductId()).orElseThrow(
                () -> {
                    log.error("Product not found during add to cart api");
                    return new ResourceNotFoundException("Product not found");
                }
        );

        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProductVariant(cart, product);

        // if product is already in the cart
        if (existingItem.isPresent()) {
            log.info("Cart item already present in the user's cart");
            log.info("Updating item quantity in the cart");
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
            return cartMapper.mapToCartResponse(cart);
        }

        // fresh cart
        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .productVariant(product)
                .quantity(request.getQuantity())
                .build();
        cartItemRepository.save(cartItem);
        log.info("Item added successfully in the cart");

        return cartMapper.mapToCartResponse(cart);
    }



    public CartResponse getCart() {
        log.debug("Request received to fetch cart details");
        String currentUserEmail = securityUtil.getCurrentUserEmail();
        log.info("Extracted user email: {}", currentUserEmail);
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow(
                () -> {
                    log.error("User not found during fetching cart details");
                    throw new ResourceNotFoundException("User not found");
                }
        );
        Cart cart = user.getCart();
        if (cart == null) {
            log.warn("No cart found for the user");
            throw new ResourceNotFoundException("Cart not found");
        }
        log.info("Cart successfully fetched for the user");
        return cartMapper.mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItem(UUID id, CartItemUpdateRequest request) {
        log.debug("Request received to update cart item with id: {}", id);
        log.debug(request.toString());
        CartItem item = cartItemRepository.findById(id).orElseThrow(
                () -> {
                    log.error("Cart item not found");
                    throw new ResourceNotFoundException("Cart item not found");
                }
        );
        securityUtil.validateCurrentUser(item.getCart().getUser().getEmail());

        if (request.getQuantity() == 0) {
            log.info("Item quantity in request is zero. Proceeding to delete the cart item");
            cartItemRepository.delete(item);
            log.info("Cart item deleted successfully");
            return getCart();
        }
        item.setQuantity(request.getQuantity());
        CartItem saved = cartItemRepository.save(item);

        log.info("Cart item updated successfully");
        return cartMapper.mapToCartResponse(saved.getCart());
    }

    @Transactional
    public void removeCartItem(UUID id) {
        log.debug("Request received to delete cart item");
        CartItem item = cartItemRepository.findById(id).orElseThrow(
                () -> {
                    log.error("Cart item not found during cart item delete api");
                    throw new ResourceNotFoundException("Cart item not found");
                }
        );
        securityUtil.validateCurrentUser(item.getCart().getUser().getEmail());
        cartItemRepository.delete(item);
        log.info("Cart item deleted successfully");
    }

    public CartItem getCartItem(UUID cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId).orElseThrow(
                () -> {
                    log.error("Cart item not found during cart item delete api");
                    throw new ResourceNotFoundException("Cart item not found");
                }
        );
        return item;
    }
}