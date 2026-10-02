package com.backend.ShopSphere.security;

import com.backend.ShopSphere.dto.TokenValidationResult;
import com.backend.ShopSphere.exception.UnauthorizedException;
import com.backend.ShopSphere.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");


        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (token.trim().isEmpty()) {
            log.warn("Empty token provided");
            request.setAttribute("exception", new UnauthorizedException("Token cannot be empty"));
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Try to authenticate
        try {
            TokenValidationResult validationResult = jwtService.validateTokenComprehensive(token);

            if (!validationResult.isValid()) {
                log.error("Invalid token found");
                throw new UnauthorizedException("Invalid token");
            }

            if (validationResult.isExpired()) {
                log.error("Token is expired");
                throw new UnauthorizedException("Token expired");
            }

            String email = validationResult.getUsername();
            if (email == null) {
                log.error("Email not found in the token");
                throw new UnauthorizedException("Invalid token");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // ✅ Extract user ID from token claims
            UUID userId = null;
            if (validationResult.getClaims() != null) {
                String userIdStr = validationResult.getClaims().get("userId", String.class);
                if (userIdStr != null) {
                    userId = UUID.fromString(userIdStr);
                }
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // ✅ Store user ID in authentication details
            Map<String, Object> details = new HashMap<>();
            details.put("userId", userId);
            details.put("email", email);
            authToken.setDetails(details);

            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("Authenticated User: {}", userDetails.getUsername());
            log.debug("Authorities: {}", userDetails.getAuthorities());

        } catch (UnauthorizedException e) {
            log.warn("Authentication failed for {}: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
            request.setAttribute("exception", e);
        }

        // Continue filter chain in ALL cases (authentication success or failure)
        filterChain.doFilter(request, response);
    }
}
