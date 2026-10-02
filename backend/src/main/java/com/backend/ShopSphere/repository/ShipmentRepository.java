package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.entity.Shipment;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.enums.ShipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findByOrder(Order order);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    Optional<Shipment> findBySellerOrderItemIdAndType(UUID sellerOrderItemId, ShipmentType type);

    boolean existsBySellerOrderItemIdAndType(UUID sellerOrderItemId, ShipmentType type);
}
