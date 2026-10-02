package com.backend.ShopSphere.entity;

import com.backend.ShopSphere.common.BaseEntity;
import com.backend.ShopSphere.enums.CancellationReason;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name="order_items")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="order_item_id", columnDefinition = "CHAR(36)")
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_id")
    private Order order;

    @OneToOne(mappedBy = "orderItem", fetch = FetchType.LAZY)
    private SellerOrderItem sellerOrderItem;

    @OneToOne(mappedBy = "orderItem", fetch = FetchType.LAZY)
    private Return returnEntity;

    //provide proper order-item and product relationship
    private UUID productVariantId;
    private String sku;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> attributes;

    @Column(precision = 19, scale = 2)
    private BigDecimal productPrice;

    private Integer quantity;

    @Column(precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    private FulfillmentStatus status;

    @Column(nullable = false)
    private boolean isReturnApplicable;

    private Integer returnWindowDays;

    //private Integer returnedQuantity = 0;

    private LocalDateTime cancelledAt;
    private LocalDateTime returnedAt;
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    private CancellationReason cancellationReason;
}
