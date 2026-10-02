package com.backend.ShopSphere.entity;

import com.backend.ShopSphere.common.BaseEntity;
import com.backend.ShopSphere.enums.RefundApprovalMode;
import com.backend.ShopSphere.enums.ReturnReason;
import com.backend.ShopSphere.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "returns",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_return_order_item",
                columnNames = "order_item_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Return extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "return_id", columnDefinition = "CHAR(36)")
    @Setter(AccessLevel.NONE)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_item_id",
            nullable = false,
            unique = true
    )
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnReason reason;

    @Enumerated(EnumType.STRING)
    private RefundApprovalMode refundApprovalMode;

    @Column(length = 1000)
    private String customerComment;

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;

    @Column(length = 1000)
    private String rejectionReason;
}
