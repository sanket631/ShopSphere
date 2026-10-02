package com.backend.ShopSphere.security;

import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.exception.ForbiddenException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.exception.UnauthorizedException;
import com.backend.ShopSphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        // Try to get user ID from authentication details
        Object details = authentication.getDetails();
        if (details instanceof Map) {
            Map<?, ?> detailsMap = (Map<?, ?>) details;
            Object userIdObj = detailsMap.get("userId");
            if (userIdObj instanceof UUID) {
                return (UUID) userIdObj;
            }
            if (userIdObj instanceof String) {
                return UUID.fromString((String) userIdObj);
            }
        }

        // Fallback: get from database using email
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        return authentication.getName();
    }

    private boolean validateRole(String role){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication.getAuthorities()
                .stream()
                .anyMatch(
                        authority ->
                                authority.getAuthority().equals(role)
                );
    }

    public boolean isAdmin(){
        return validateRole("ROLE_ADMIN");
    }

    public boolean isSeller(){
        return validateRole("ROLE_SELLER");
    }

    public boolean isCustomer(){
        return validateRole("ROLE_CUSTOMER");
    }
    public void validateProductOwnership(String ownerEmail){
        log.debug("Request received to validate product ownership");
        if(isAdmin()) {
            log.info("User identified as ADMIN");
            return;
        }

        log.info("Seller email in request: {}", ownerEmail);
        String userNameExtracted = getCurrentUserEmail();
        log.info("Extracted seller email: {}", userNameExtracted);
        if(!ownerEmail.equals(userNameExtracted)){
            log.warn("Seller mismatch");
            throw new ForbiddenException("You are not allowed to perform this activity");
        }

        log.info("Product ownership validated successfully");
    }

    public void validateCurrentUser(String ownerEmail){
        log.debug("Validating current user");
        if(isAdmin()){
            log.info("User identified as ADMIN");
            return;
        }
        log.info("User email in request: {}", ownerEmail);
        String currentUser = getCurrentUserEmail();
        log.info("Extracted user email: {}", currentUser);
        if(!ownerEmail.equals(currentUser)){
            log.warn("User mismatch");
            throw new ForbiddenException(
                    "Access denied"
            );
        }

        log.info("User validated successfully");
    }

    public String getCurrentUserRole() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElse(null);
    }
}
