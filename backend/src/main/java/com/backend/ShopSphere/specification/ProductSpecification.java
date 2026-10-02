package com.backend.ShopSphere.specification;

import com.backend.ShopSphere.entity.Product;
import com.backend.ShopSphere.entity.ProductVariant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ProductSpecification {

    public static Specification<Product> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("brand")), pattern),
                    cb.like(cb.lower(root.get("shortDescription")), pattern),
                    cb.like(cb.lower(root.get("longDescription")), pattern)
            );
        };
    }

    public static Specification<Product> hasBrand(String brand) {
        return (root, query, criteriaBuilder) -> {
            if (brand == null || brand.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("brand")),
                    brand.toLowerCase()
            );
        };
    }

    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("category").get("id"),
                    categoryId
            );
        };
    }

    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            assert query != null;
            query.distinct(true);

            Join<Product, ProductVariant> variant = root.join("variants");

            List<Predicate> predicates = new ArrayList<>();

            if (min != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(variant.get("sellingPrice"), min)
                );
            }

            if (max != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(variant.get("sellingPrice"), max)
                );
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }

    public static Specification<Product> inStock() {
        return (root, query, cb) -> {
            Join<Product, ProductVariant> variant = root.join("variants");
            return cb.greaterThan(
                    variant.get("availableStock"),
                    0
            );
        };
    }
}