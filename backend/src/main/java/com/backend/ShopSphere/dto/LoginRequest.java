package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                ", password='" + maskPassword(password) + '\'' +
                '}';
    }

    private String maskPassword(String password) {
        if (password == null) return "null";
        if (password.isEmpty()) return "empty";
        return "********";  // Always show same length
    }
}
