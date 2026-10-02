package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewImageRequest {

    private String fileName;

    private String fileType;

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    private Long fileSize;
}
