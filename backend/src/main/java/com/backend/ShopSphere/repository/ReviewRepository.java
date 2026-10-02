package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.Product;
import com.backend.ShopSphere.entity.Review;
import com.backend.ShopSphere.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByUserAndProduct(User user,Product product);

    Optional<Review> findByIdAndUser(UUID reviewId,User user);

    Page<Review> findByProduct(Product product,Pageable pageable);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product = :product GROUP BY r.rating")
    List<Object[]> countByRatingGrouped(@Param("product") Product product);
}