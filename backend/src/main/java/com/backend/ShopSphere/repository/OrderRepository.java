package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.enums.OrderStatus;
import com.backend.ShopSphere.enums.PaymentStatus;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Query("SELECT o FROM Order o WHERE o.payment.status = :paymentStatus AND o.reservationExpiry < :now")
    List<Order> findPendingPaymentWithExpiredReservation(
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("now") LocalDateTime now
    );

    long countByUserAndStatus(User user, OrderStatus status);
}
