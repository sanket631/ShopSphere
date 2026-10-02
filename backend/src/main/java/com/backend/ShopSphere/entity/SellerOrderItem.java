package com.backend.ShopSphere.entity;

import com.backend.ShopSphere.common.BaseEntity;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="seller_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerOrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="seller_order_item_id", columnDefinition = "CHAR(36)")
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="seller_id")
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_id")
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    private UUID productVariantId;
    private String sku;
    private Integer quantity;
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    private FulfillmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private Integer shippedQuantity = 0;
    private Integer cancelledQuantity = 0;
    private Integer returnedQuantity = 0;

    @OneToMany(mappedBy = "sellerOrderItem", fetch = FetchType.LAZY)
    private List<Shipment> shipments = new ArrayList<>();
}
