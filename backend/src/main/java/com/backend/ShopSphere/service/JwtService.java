package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.TokenValidationResult;
import com.backend.ShopSphere.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            // Token is expired but we can still extract the username
            log.debug("Token expired during extracting username");
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            // Token is invalid (wrong signature, malformed, etc.)
            log.warn("Invalid token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error extracting username: {}", e.getMessage());
            return null;
        }
    }

    public boolean isValid(String token, String email) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check if username matches
            boolean usernameMatch = claims.getSubject().equals(email);

            // Check if token is not expired
            Date expiration = claims.getExpiration();
            boolean isExpired = expiration.before(new Date());

            return usernameMatch && !isExpired;

        } catch (ExpiredJwtException e) {
            log.debug("Token expired during validation");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid token during validation: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during validation: {}", e.getMessage());
            return false;
        }
    }


    // Optional: Helper method to get claims without throwing exceptions
    public Optional<Claims> getClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public TokenValidationResult validateTokenComprehensive(String token){
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            boolean isExpired = expiration.before(new Date());

            return TokenValidationResult.builder()
                    .valid(true)
                    .expired(isExpired)
                    .username(claims.getSubject())
                    .claims(claims)
                    .build();

        } catch (ExpiredJwtException e) {
            // Token is expired but we can still extract info
            return TokenValidationResult.builder()
                    .valid(true)
                    .expired(true)
                    .username(e.getClaims().getSubject())
                    .claims(e.getClaims())
                    .build();

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return TokenValidationResult.builder()
                    .valid(false)
                    .expired(false)
                    .username(null)
                    .build();
        }
    }
}