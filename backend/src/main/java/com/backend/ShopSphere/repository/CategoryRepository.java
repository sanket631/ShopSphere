package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Category;
import com.backend.ShopSphere.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
    Optional<Category> findBySlug(String slug);

    Optional<Category> findByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    List<Category> findByParentCategoryIsNull();

    List<Category> findByParentCategoryId(UUID parentCategoryId);

    List<Category> findByPathStartingWith(String path);
}
