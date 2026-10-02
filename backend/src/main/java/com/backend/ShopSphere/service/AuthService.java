package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.entity.RefreshToken;
import com.backend.ShopSphere.enums.Role;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.exception.ConflictException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.RefreshTokenRepository;
import com.backend.ShopSphere.repository.UserRepository;
import com.backend.ShopSphere.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.backend.ShopSphere.exception.UnauthorizedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityUtil securityUtil;

    public void registerCustomer(RegisterRequest request){
        log.debug("Request received to register Customer: {}", request.toString());
        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            log.warn("Customer exists with the same email address");
            throw new ConflictException("This email address already exist");
        }

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(user);
        log.info("Customer registered successfully");
    }

    public void registerSeller(RegisterRequest request){
        log.debug("Request received to register Seller: {}", request.toString());
        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            log.warn("Seller exists with the same email address");
            throw new ConflictException("This email address already exist");
        }

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.SELLER)
                .build();
        userRepository.save(user);
        log.info("Seller registered successfully");
    }

    public AuthResponse login(LoginRequest request){
        try {
            log.debug("Login request received: {}", request.toString());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                    () -> new ResourceNotFoundException("User not found")
            );
            String accessToken = jwtService.generateToken(user);
            RefreshToken refreshToken = refreshTokenService.creataRefreshToken(user);
            log.info("Login successful");
            log.info("Generated access token: {}", accessToken);
            log.info("Refresh token generated successfully");
            return new AuthResponse(accessToken, refreshToken.getToken());
        } catch (AuthenticationException e) {
            log.error("Error occurred during authentication: {}", e.getCause().getMessage());
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest request){
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();
        String accessToken = jwtService.generateToken(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    @Transactional
    public void logout(String token){
        log.info("Processing logout request");

        TokenValidationResult validationResult = jwtService.validateTokenComprehensive(token);

        if (!validationResult.isValid()) {
            log.warn("Invalid token provided for logout");
            throw new UnauthorizedException("Invalid token");
        }

        if (validationResult.isExpired()) {
            log.warn("Expired token provided for logout from user: {}", validationResult.getUsername());
            throw new UnauthorizedException("Token is expired. Please login again to logout.");
        }

        String currentUserEmail = validationResult.getUsername();
        if (currentUserEmail == null) {
            throw new UnauthorizedException("Invalid token: No user identifier");
        }

        // 2. Find user
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> {
                    log.error("User not found during logout: {}", currentUserEmail);
                    return new ResourceNotFoundException("User not found for logout");
                });

        // 3. Delete refresh token (if exists)
        RefreshToken refreshToken = user.getRefreshToken();
        if (refreshToken == null) {
            log.info("User {} had no refresh token to delete (already logged out?)", currentUserEmail);
        } else {
            refreshTokenRepository.deleteByUser(user);
            log.info("Refresh token deleted for user: {}", currentUserEmail);
        }

        // 4. Optional: Blacklist access token
        // blacklistService.addToBlacklist(token, jwtService.getExpirationDate(token));

        log.info("User logged out successfully: {}", currentUserEmail);
    }
}
