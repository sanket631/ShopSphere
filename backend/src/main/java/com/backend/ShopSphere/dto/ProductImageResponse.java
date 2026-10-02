package com.backend.ShopSphere.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProductImageResponse {
    private UUID imageId;

    private String imageUrl;

    private String fileName;

    private Boolean primaryImage;

    private Integer displayOrder;
}
