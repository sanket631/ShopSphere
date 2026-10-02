package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
    @NotBlank
    private String fullName;

    @NotBlank
    @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Invalid phone number"
    )
    private String phoneNumber;

    private AddressType type;

    private String landmark;

    private String label;

    @NotBlank
    private String addressLine1;

    private String addressLine2;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "Provide a valid postal code")
    private String postalCode;

    @NotBlank
    private String country;

    private Boolean isDefault;
}
