package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register/customer")
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody RegisterRequest request){
        log.info("=== REGISTER CUSTOMER API CALLED ===");
        log.debug("Customer registration request - email: {}, fullName: {}, phoneNumber: {}",
                maskEmail(request.getEmail()), request.getFullName(), request.getPhoneNumber());

        authService.registerCustomer(request);

        log.info("Customer registered successfully - email: {}", maskEmail(request.getEmail()));

        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/register/seller")
    public ResponseEntity<?> registerSeller(@Valid @RequestBody RegisterRequest request){
        log.info("=== REGISTER SELLER API CALLED ===");
        log.debug("Seller registration request - email: {}, fullName: {}, phoneNumber: {}",
                maskEmail(request.getEmail()), request.getFullName(), request.getPhoneNumber());

        authService.registerSeller(request);

        log.info("Seller registered successfully - email: {}", maskEmail(request.getEmail()));

        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request){
        log.info("=== LOGIN API CALLED ===");
        log.debug("Login attempt for email: {}", maskEmail(request.getEmail()));

        AuthResponse response = authService.login(request);

        log.info("Login successful for email: {}", maskEmail(request.getEmail()));
        log.debug("Auth response - token generated, token type: Bearer");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request){
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(){
        String accessToken = extractAccessToken();
        if(accessToken == null) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(401)
                    .message("Token is required for logout")
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        authService.logout(accessToken);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private String extractAccessToken() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // Utility method to mask email for logging
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "null";
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "***";
        }
        String localPart = parts[0];
        if (localPart.length() <= 2) {
            return "***@" + parts[1];
        }
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + parts[1];
    }
}