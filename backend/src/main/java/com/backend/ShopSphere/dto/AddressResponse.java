package com.backend.ShopSphere.dto;

import com.backend.ShopSphere.enums.AddressType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Builder
public class AddressResponse {
    private UUID addressId;

    private String fullName;

    private String phoneNumber;

    private AddressType addressType;

    private String label;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String landmark;

    private Boolean isDefault;
}
