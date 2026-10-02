package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.AddressRequest;
import com.backend.ShopSphere.dto.AddressResponse;
import com.backend.ShopSphere.entity.Address;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.exception.ForbiddenException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.exception.UnauthorizedException;
import com.backend.ShopSphere.mapper.AddressMapper;
import com.backend.ShopSphere.repository.AddressRepository;
import com.backend.ShopSphere.repository.UserRepository;
import com.backend.ShopSphere.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final SecurityUtil securityUtil;
    private final AddressMapper addressMapper;

    // ==================== CREATE ====================

    @Transactional
    public AddressResponse createAddress(AddressRequest request) {
        log.debug("Request received to create new address: {}", request);

        UUID userId = securityUtil.getCurrentUserId();
        log.debug("Current user ID: {}", userId);

        List<Address> existingAddresses = addressRepository.findByUserId(userId);
        log.debug("User has {} existing addresses", existingAddresses.size());

        boolean shouldBeDefault = existingAddresses.isEmpty() ? Boolean.TRUE : request.getIsDefault();

        if (shouldBeDefault && !existingAddresses.isEmpty()) {
            log.info("Setting new address as default. Updating existing addresses to non-default");
            updateExistingDefaultAddresses(existingAddresses);
        }

        Address address = Address.builder()
                .user(getUserReference(userId))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .type(request.getType())
                .label(request.getLabel())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .landmark(request.getLandmark())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(shouldBeDefault)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Address created successfully - addressId={}, userId={}, isDefault={}",
                saved.getId(), userId, saved.getIsDefault());

        return addressMapper.mapToAddressResponse(saved);
    }

    // ==================== READ ====================

    public AddressResponse getAddress(UUID addressId) {
        log.debug("Fetching address with id={}", addressId);

        Address address = findAndValidateOwnership(addressId);
        log.info("Address retrieved successfully - addressId={}", addressId);

        return addressMapper.mapToAddressResponse(address);
    }

    public List<AddressResponse> getMyAddresses() {
        log.debug("Fetching all addresses for current user");

        UUID userId = securityUtil.getCurrentUserId();
        log.debug("Current user ID: {}", userId);

        List<Address> addresses = addressRepository.findByUserId(userId);
        log.info("Retrieved {} addresses for user", addresses.size());

        if (addresses.isEmpty()) {
            log.warn("No addresses found for user");
        } else {
            long defaultCount = addresses.stream().filter(Address::getIsDefault).count();
            log.debug("Found {} total addresses, {} default addresses", addresses.size(), defaultCount);
        }

        return addresses.stream()
                .map(addressMapper::mapToAddressResponse)
                .toList();
    }

    // ==================== UPDATE ====================

    @Transactional
    public AddressResponse updateAddress(UUID addressId, AddressRequest request) {
        log.debug("Updating address with id={}", addressId);

        Address address = findAndValidateOwnership(addressId);
        UUID userId = securityUtil.getCurrentUserId();

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            log.info("Setting address {} as default", addressId);

            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(oldDefault -> {
                        oldDefault.setIsDefault(false);
                        log.debug("Removed default status from addressId={}", oldDefault.getId());
                    });

            address.setIsDefault(true);
        }

        updateAddressFields(address, request);

        Address saved = addressRepository.save(address);
        log.info("Address updated successfully - addressId={}", saved.getId());

        return addressMapper.mapToAddressResponse(saved);
    }

    @Transactional
    public void setDefaultAddress(UUID addressId) {
        log.debug("Setting address {} as default", addressId);
        Address address = findAndValidateOwnership(addressId);
        UUID userId = securityUtil.getCurrentUserId();

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            log.debug("Address {} is already default", addressId);
            return;
        }

        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(oldDefault -> {
                    oldDefault.setIsDefault(false);
                    log.debug("Removed default status from addressId={}", oldDefault.getId());
                });

        address.setIsDefault(true);
        addressRepository.save(address);

        log.info("Address {} set as default", addressId);
    }

    // ==================== DELETE ====================

    @Transactional
    public void deleteAddress(UUID addressId) {
        log.debug("Deleting address with id={}", addressId);

        Address address = findAndValidateOwnership(addressId);
        UUID userId = securityUtil.getCurrentUserId();
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        addressRepository.delete(address);
        log.info("Address deleted successfully - addressId={}, userId={}, wasDefault={}",
                addressId, userId, wasDefault);

        if (wasDefault) {
            log.warn("Default address deleted for userId={}", userId);
            setNewDefaultIfNeeded(userId);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Find address and validate ownership in one method
     * Reduces code duplication and ensures consistent validation
     */
    private Address findAndValidateOwnership(UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> {
                    log.error("Address not found with id={}", addressId);
                    return new ResourceNotFoundException("Address not found");
                });

        UUID currentUserId = securityUtil.getCurrentUserId();
        UUID addressOwnerId = address.getUser().getId();

        if (!currentUserId.equals(addressOwnerId)) {
            log.warn("User {} attempted to access address {} owned by {}",
                    currentUserId, addressId, addressOwnerId);
            throw new ForbiddenException("You are not authorized to access this address");
        }

        return address;
    }

    /**
     * Update existing default addresses to non-default
     */
    private void updateExistingDefaultAddresses(List<Address> existingAddresses) {
        int updatedCount = 0;
        for (Address addr : existingAddresses) {
            if (Boolean.TRUE.equals(addr.getIsDefault())) {
                addr.setIsDefault(false);
                updatedCount++;
                log.debug("Removed default status from addressId={}", addr.getId());
            }
        }

        if (updatedCount > 0) {
            addressRepository.saveAll(existingAddresses);
            log.info("Updated {} existing addresses to non-default", updatedCount);
        }
    }

    /**
     * Update address fields from request
     */
    private void updateAddressFields(Address address, AddressRequest request) {
        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setType(request.getType());
        address.setLabel(request.getLabel());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        // Only update isDefault if explicitly set in request
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
    }

    /**
     * Set a new default address if user has remaining addresses
     */
    private void setNewDefaultIfNeeded(UUID userId) {
        List<Address> remainingAddresses = addressRepository.findByUserId(userId);

        if (remainingAddresses.isEmpty()) {
            log.info("User has no remaining addresses after deletion");
        } else {
            Address newDefault = remainingAddresses.get(0);
            newDefault.setIsDefault(Boolean.TRUE);
            addressRepository.save(newDefault);
            log.info("Address {} set as default after deleting previous default", newDefault.getId());
        }
    }

    /**
     * Get User reference WITHOUT database call
     * Only sets the ID field
     */
    private User getUserReference(UUID userId) {
        return User.builder()
                .id(userId)
                .build();
    }
    
}