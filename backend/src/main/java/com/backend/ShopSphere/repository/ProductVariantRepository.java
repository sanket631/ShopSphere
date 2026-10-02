package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Product;
import com.backend.ShopSphere.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID>, JpaSpecificationExecutor<ProductVariant> {
    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    List<ProductVariant> findByProduct(Product product);

    List<ProductVariant> findByProductId(UUID productId);

    List<ProductVariant> findByAvailableStockGreaterThan(Integer stock);

    List<ProductVariant> findBySellingPriceLessThan(BigDecimal maxPrice);

    List<ProductVariant> findBySellingPriceGreaterThan(BigDecimal minPrice);

    List<ProductVariant> findBySellingPriceBetween(BigDecimal minPrice,
                                                   BigDecimal maxPrice);
}
