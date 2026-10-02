package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
            String entityType,
            UUID entityId
    );

    List<AuditLog> findByEntityIdOrderByOccurredAtDesc(UUID entityId);
}
