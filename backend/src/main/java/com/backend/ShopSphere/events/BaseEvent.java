package com.backend.ShopSphere.events;

import com.backend.ShopSphere.enums.EventType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentSuccessEvent.class, name = "PAYMENT_SUCCESS"),
        @JsonSubTypes.Type(value = PaymentFailedEvent.class, name = "PAYMENT_FAILURE"),
        @JsonSubTypes.Type(value = ReservationExpiredEvent.class, name = "RESERVATION_EXPIRED"),
        @JsonSubTypes.Type(value = OrderItemCancelledEvent.class, name = "ORDER_ITEM_CANCELLED"),
        @JsonSubTypes.Type(value = ShipmentStatusChangedEvent.class, name = "SHIPMENT_STATUS_CHANGED"),
        @JsonSubTypes.Type(value = ReviewCreatedEvent.class, name = "REVIEW_CREATED"),
        @JsonSubTypes.Type(value = ReviewUpdatedEvent.class, name = "REVIEW_UPDATED"),
        @JsonSubTypes.Type(value = ReviewDeletedEvent.class, name = "REVIEW_DELETED"),
        @JsonSubTypes.Type(value = RefundStatusChangedEvent.class, name = "REFUND_STATUS_CHANGED"),
        @JsonSubTypes.Type(value = ReturnStatusChangedEvent.class, name = "RETURN_STATUS_CHANGED"),
        @JsonSubTypes.Type(value = OrderItemReturnEvent.class, name = "ORDER_ITEM_RETURN"),
        @JsonSubTypes.Type(value = ReturnStatusChangedEvent.class, name = "RETURN_STATUS_CHANGED")
})
public class BaseEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    protected UUID eventId;
    protected EventType eventType;
    protected LocalDateTime createdAt;
    protected int retryCount;
    protected UUID orderId;
    protected UUID userId;
    protected String correlationId;
    private LocalDateTime publishedAt;

    public BaseEvent(EventType eventType, UUID orderId, UUID userId) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.orderId = orderId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
        this.correlationId = UUID.randomUUID().toString();
        this.publishedAt = LocalDateTime.now();
    }
}