package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.entity.WishListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishListItemRepository extends JpaRepository<WishListItem,UUID> {
    List<WishListItem> findByUserId(UUID userId);

    boolean existsByUserIdAndProductVariantId(UUID userId, UUID productVariantId);

    Optional<WishListItem> findByUserIdAndProductVariantId(UUID userId, UUID productVariantId);
}
