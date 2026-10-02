package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewImagesRepository extends JpaRepository<ReviewImage, UUID> {
}
