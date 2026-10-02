package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Category;
import com.backend.ShopSphere.entity.Product;
import com.backend.ShopSphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> , JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Product> findByCategory(Category category);

    // Custom query for price sorting with null safety
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.variants v " +
            "WHERE p IN :products " +
            "AND v IS NOT NULL " +
            "AND v.sellingPrice IS NOT NULL " +
            "GROUP BY p " +
            "ORDER BY MIN(v.sellingPrice) ASC")
    List<Product> findProductsSortedByMinPriceAsc(@Param("products") List<Product> products);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.variants v " +
            "WHERE p IN :products " +
            "AND v IS NOT NULL " +
            "AND v.sellingPrice IS NOT NULL " +
            "GROUP BY p " +
            "ORDER BY MIN(v.sellingPrice) DESC")
    List<Product> findProductsSortedByMinPriceDesc(@Param("products") List<Product> products);

    List<Product> findByCategoryId(UUID categoryId);

    List<Product> findBySeller(User seller);

    List<Product> findBySellerId(UUID sellerId);

    List<Product> findByBrandIgnoreCase(String brand);

    List<Product> findByNameContainingIgnoreCase(String keyword);
}
