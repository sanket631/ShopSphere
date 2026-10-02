package com.backend.ShopSphere.mapper;

import com.backend.ShopSphere.dto.AddressResponse;
import com.backend.ShopSphere.dto.ShippingAddressResponse;
import com.backend.ShopSphere.entity.Address;
import com.backend.ShopSphere.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AddressMapper {

    public AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.builder()
                .addressId(address.getId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .addressType(address.getType())
                .label(address.getLabel())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .landmark(address.getLandmark())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .build();
    }

    public ShippingAddressResponse mapShippingAddress(Order order) {
        log.trace("Mapping shipping address from order - Order ID: {}", order.getId());

        ShippingAddressResponse response = ShippingAddressResponse.builder()
                .fullName(order.getShippingFullName())
                .phoneNumber(order.getShippingPhoneNumber())
                .addressLine1(order.getShippingAddressLine1())
                .addressLine2(order.getShippingAddressLine2())
                .landmark(order.getShippingLandmark())
                .city(order.getShippingCity())
                .state(order.getShippingState())
                .postalCode(order.getShippingPostalCode())
                .country(order.getShippingCountry())
                .build();

        log.trace("Shipping address mapped - Name: {}, City: {}",
                response.getFullName(), response.getCity());
        return response;
    }
}
