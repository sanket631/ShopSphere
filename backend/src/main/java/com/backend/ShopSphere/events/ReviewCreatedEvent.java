package com.backend.ShopSphere.events;

import com.backend.ShopSphere.enums.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ReviewCreatedEvent extends BaseEvent{
    private UUID reviewId;
    private UUID productId;

    public ReviewCreatedEvent(UUID orderId, UUID userId, UUID reviewId, UUID productId) {
        super(EventType.REVIEW_CREATED, orderId, userId);
        this.reviewId = reviewId;
        this.productId = productId;
    }
}
