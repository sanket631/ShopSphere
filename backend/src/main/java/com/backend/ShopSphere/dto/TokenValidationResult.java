package com.backend.ShopSphere.dto;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Getter
@Service
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenValidationResult {
    private boolean valid;
    private boolean expired;
    private String username;
    private Claims claims;
}
