package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.entity.OrderItem;
import com.backend.ShopSphere.entity.Refund;
import com.backend.ShopSphere.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    boolean existsByOrderItemId(UUID orderItemId);
    @Query("""
            SELECT r
            FROM Refund r
            JOIN r.orderItem oi
            JOIN oi.sellerOrderItem soi
            WHERE soi.seller.id = :sellerId
            ORDER BY r.requestedAt DESC
            """)
    Page<Refund> findSellerRefunds(
            UUID sellerId,
            Pageable pageable
    );

    @Query("""
            SELECT r
            FROM Refund r
            JOIN r.orderItem oi
            JOIN oi.sellerOrderItem soi
            WHERE soi.seller.id = :sellerId
              AND r.status = :status
            ORDER BY r.requestedAt ASC
            """)
    Page<Refund> findSellerRefundsByStatus(
            UUID sellerId,
            RefundStatus status,
            Pageable pageable
    );
}
