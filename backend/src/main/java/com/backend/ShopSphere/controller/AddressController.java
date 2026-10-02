package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.AddressRequest;
import com.backend.ShopSphere.dto.AddressResponse;
import com.backend.ShopSphere.service.AddressService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody AddressRequest request){
        log.info("=== CREATE ADDRESS API CALLED ===");
        log.debug("Create address request received - city: {}, state: {}, country: {}, isDefault: {}",
                request.getCity(), request.getState(), request.getCountry(), request.getIsDefault());

        AddressResponse response = addressService.createAddress(request);

        log.info("Address created successfully - addressId: {}, isDefault: {}",
                response.getAddressId(), response.getIsDefault());
        log.debug("Created address details: {}", response);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<?> getAddress(@PathVariable UUID addressId){
        log.info("=== GET Address API CALLED ===");
        log.debug("Fetching address with id: {}", addressId);

        return ResponseEntity.ok(addressService.getAddress(addressId));
    }

    @GetMapping
    public ResponseEntity<?> getMyAddresses(){
        log.info("=== GET MY ADDRESSES API CALLED ===");
        log.debug("Fetching all addresses for current user");

        List<AddressResponse> myAddresses = addressService.getMyAddresses();

        int addressCount = myAddresses.size();
        long defaultCount = myAddresses.stream().filter(AddressResponse::getIsDefault).count();

        if(addressCount == 0) {
            log.info("No addresses found for current user");
        } else {
            log.info("Addresses retrieved successfully - totalAddresses: {}, defaultAddresses: {}",
                    addressCount, defaultCount);

            log.debug("Address list summary:");
            myAddresses.forEach(address ->
                    log.debug("  - AddressId: {}, City: {}, State: {}, IsDefault: {}",
                            address.getAddressId(), address.getCity(), address.getState(), address.getIsDefault())
            );
        }

        return ResponseEntity.ok(myAddresses);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request){

        return ResponseEntity.ok(addressService.updateAddress(addressId, request));
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<Void> setDefaultAddress(@PathVariable UUID addressId){

        addressService.setDefaultAddress(addressId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> delete(@PathVariable UUID addressId){
        log.info("=== DELETE ADDRESS API CALLED ===");
        log.debug("Delete address request - addressId: {}", addressId);

        addressService.deleteAddress(addressId);

        log.info("Address deleted successfully - addressId: {}", addressId);

        return ResponseEntity.noContent().build();
    }
}