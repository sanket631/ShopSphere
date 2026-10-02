package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.SellerOrderItem;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface SellerOrderRepository extends JpaRepository<SellerOrderItem, UUID> {
    List<SellerOrderItem> findBySeller(User user);

    Page<SellerOrderItem> findBySeller_Id(
            UUID sellerId,
            Pageable pageable
    );

    Optional<SellerOrderItem> findByIdAndSeller_Id(
            UUID sellerOrderId,
            UUID sellerId
    );

    Page<SellerOrderItem> findBySeller_IdAndStatus(
            UUID sellerId,
            FulfillmentStatus status,
            Pageable pageable
    );
}
