package com.backend.ShopSphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @Email(message = "Provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Phone number must be in E.164 format. Example: +919876543210"
    )
    private String phoneNumber;

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", password='" + maskPassword(password) + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }

    private String maskPassword(String password) {
        if (password == null) {
            return "null";
        }
        if (password.isEmpty()) {
            return "";
        }
        return "*".repeat(Math.min(password.length(), 8));
    }

}
