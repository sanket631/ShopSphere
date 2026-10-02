package com.backend.ShopSphere.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditRequest {
    private String entityType;
    private UUID entityId;
    private String oldStatus;
    private String newStatus;
    private String action;
    private UUID performedBy;
    private String performedByType;
    private String reason;
}
