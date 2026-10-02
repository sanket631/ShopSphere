package com.backend.ShopSphere.repository;

import com.backend.ShopSphere.entity.RefreshToken;
import com.backend.ShopSphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    @Transactional
    @Modifying
    @Query("""
DELETE FROM RefreshToken rt
WHERE rt.user = :user
""")
    void deleteByUser(
            @Param("user") User user
    );
}
