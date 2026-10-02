package com.backend.ShopSphere.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewResponse {
    private UUID reviewId;

    private UUID userId;

    private String userName;

    private Integer rating;

    private String description;

    private List<String> reviewImages;

    private LocalDateTime reviewDate;

    private Boolean verifiedPurchase;

    private String purchasedVariantSku;

    private Map<String,String> variantAttributes;
}
