package com.backend.ShopSphere.entity;

import com.backend.ShopSphere.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_processed_event_consumer",
                columnNames = {"event_id", "consumer"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "processed_event_id", columnDefinition = "CHAR(36)")
    @Setter(AccessLevel.NONE)
    private UUID id;

    @JdbcTypeCode(Types.CHAR)
    @Column(nullable = false, length = 36)
    private UUID eventId;

    @Column(nullable = false)
    private String consumer;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
}
