package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReviewRequest {
    @NotBlank(message = "Order item Id is required")
    private UUID orderItemId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 500, message = "Review description cannot exceed 500 characters")
    private String description;

    @Size(max = 5, message = "A review can contain at most 5 images")
    private List<ReviewImageRequest> reviewImageRequests;
}