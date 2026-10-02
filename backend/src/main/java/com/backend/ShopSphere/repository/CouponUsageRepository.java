package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Coupon;
import com.backend.ShopSphere.entity.CouponUsage;
import com.backend.ShopSphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {
    long countByCoupon(Coupon coupon);

    long countByCouponAndUser(Coupon coupon, User user);

    boolean existsByCouponAndUser(Coupon coupon, User user);

    List<CouponUsage> findByUser(User user);
}
