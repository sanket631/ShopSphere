package com.backend.ShopSphere.entity;

import com.backend.ShopSphere.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_slug", columnList = "slug", unique = true),
        @Index(name = "idx_category_parent", columnList = "parent_category_id"),
        @Index(name = "idx_category_path", columnList = "path") // Crucial for LIKE queries
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "category_id", columnDefinition = "CHAR(36)")
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug; // SEO-friendly URL

    @Column(length = 500)
    private String description;

    // The level in the hierarchy (calculated, not stored as separate FK columns)
    @Column(nullable = false)
    private Integer level = 1;

    // Materialized Path - this is the secret sauce!
    // Example: "Electronics > Mobile Phones > Samsung > Galaxy S Series > S21"
    @Column(length = 500, nullable = false)
    private String path;

    // Self-referencing hierarchy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    @JsonIgnore
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Category> subCategories = new ArrayList<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Product> products;

    // For API responses only - not stored in DB
    @Transient
    private List<Category> children;

    // Helper to add sub-category
    public void addSubCategory(Category child) {
        child.setParentCategory(this);
        child.setLevel(this.level + 1);
        child.setPath(this.path + " > " + child.getName());
        this.subCategories.add(child);
    }
}
