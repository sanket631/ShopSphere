package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Cart;
import com.backend.ShopSphere.entity.CartItem;
import com.backend.ShopSphere.entity.Product;
import com.backend.ShopSphere.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCart(Cart cart);
    Optional<CartItem> findByCartAndProductVariant(Cart cart, ProductVariant product);
}
