package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Return;
import com.backend.ShopSphere.enums.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRepository extends JpaRepository<Return, UUID> {
    Optional<Return> findByOrderItemId(UUID orderItemId);

    boolean existsByOrderItemId(UUID orderItemId);

    @Query("""
        SELECT r
        FROM Return r
        JOIN r.orderItem oi
        JOIN oi.sellerOrderItem soi
        WHERE soi.seller.id = :sellerId
        ORDER BY r.requestedAt DESC
        """)
    Page<Return> findSellerReturns(UUID sellerId, Pageable pageable);

    @Query("""
        SELECT r
        FROM Return r
        JOIN r.orderItem oi
        JOIN oi.sellerOrderItem soi
        WHERE soi.seller.id = :sellerId
          AND r.status = :status
        ORDER BY r.requestedAt ASC
        """)
    Page<Return> findSellerReturnsByStatus(UUID sellerId, ReturnStatus status, Pageable pageable);
}
