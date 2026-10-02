package com.backend.ShopSphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private UUID id;

    private String name;

    private String slug;

    private String description;

    private Integer level;

    private String path;

    private UUID parentCategoryId;

    private String parentCategoryName;

    private List<CategoryResponse> children;
}
