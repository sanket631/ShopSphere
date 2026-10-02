package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.AuditRequest;
import com.backend.ShopSphere.entity.AuditLog;
import com.backend.ShopSphere.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void recordStatusChange(AuditRequest request) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .oldStatus(request.getOldStatus())
                .newStatus(request.getNewStatus())
                .action(request.getAction())
                .performedBy(request.getPerformedBy())
                .performedByType(request.getPerformedByType())
                .reason(request.getReason())
                .occurredAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);

        log.info(
                "Audit recorded. entityType={}, entityId={}, oldStatus={}, newStatus={}, action={}",
                request.getEntityType(),
                request.getEntityId(),
                request.getOldStatus(),
                request.getNewStatus(),
                request.getAction()
        );
    }
}
