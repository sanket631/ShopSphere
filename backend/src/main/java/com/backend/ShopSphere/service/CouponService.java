package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.entity.CartItem;
import com.backend.ShopSphere.entity.Coupon;
import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.enums.CouponStatus;
import com.backend.ShopSphere.enums.DiscountType;
import com.backend.ShopSphere.enums.OrderStatus;
import com.backend.ShopSphere.exception.ConflictException;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.mapper.CouponMapper;
import com.backend.ShopSphere.repository.*;
import com.backend.ShopSphere.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CouponMapper couponMapper;

    // ==================== APPLY COUPON ====================

    @Transactional
    public ApplyCouponResponse applyCoupon(CouponValidationContext context) {
        log.debug("=== APPLY COUPON STARTED ===");
        Coupon coupon = context.getCoupon();
        User user = context.getUser();
        log.debug("Applying coupon - Code: {}, User ID: {}, Order Amount: {}",
                coupon.getCode(), user.getId(), context.getTotalOrderAmount());

        validateCouponBasics(context);

        List<EligibleItem> eligibleItems = resolveEligibleItems(context);
        log.debug("Resolved {} eligible items", eligibleItems.size());

        if (eligibleItems.isEmpty()) {
            log.warn("No eligible items found for coupon: {}", coupon.getCode());
            throw new IllegalArgsException("Coupon is not applicable on selected items");
        }

        BigDecimal eligibleAmount = calculateEligibleAmount(eligibleItems);
        log.debug("Eligible amount calculated: {}", eligibleAmount);

        validateEligibleAmount(coupon, eligibleAmount);

        BigDecimal discount = calculateDiscount(coupon, eligibleAmount);
        log.info("Coupon applied - Code: {}, Discount: {}, Eligible Amount: {}",
                coupon.getCode(), discount, eligibleAmount);

        BigDecimal finalAmount = context.getTotalOrderAmount().subtract(discount);
        log.debug("Final amount after coupon: {}", finalAmount);

        ApplyCouponResponse response = ApplyCouponResponse.builder()
                .couponCode(coupon.getCode())
                .orderAmount(context.getTotalOrderAmount())
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .eligibleAmount(eligibleAmount)
                .eligibleItems(eligibleItems.size())
                .message("Coupon applied successfully")
                .build();

        log.info("Coupon application completed - Order: {}, Discount: {}, Final: {}",
                context.getTotalOrderAmount(), discount, finalAmount);
        log.debug("=== APPLY COUPON COMPLETED ===");
        return response;
    }

    // ==================== VALIDATION METHODS ====================

    private void validateEligibleAmount(Coupon coupon, BigDecimal eligibleAmount) {
        log.debug("Validating eligible amount - Coupon: {}, Eligible Amount: {}, Min Required: {}",
                coupon.getCode(), eligibleAmount, coupon.getMinimumOrderAmount());

        if (coupon.getMinimumOrderAmount() != null &&
                eligibleAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            log.warn("Minimum order amount not satisfied - Required: {}, Actual: {}",
                    coupon.getMinimumOrderAmount(), eligibleAmount);
            throw new IllegalArgsException("Minimum order amount not satisfied");
        }
        log.debug("Eligible amount validation passed");
    }

    private void validateCouponBasics(CouponValidationContext context) {
        Coupon coupon = context.getCoupon();
        User user = context.getUser();
        log.debug("Validating coupon basics - Code: {}, User: {}", coupon.getCode(), user.getId());

        // Check status
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            log.warn("Coupon is inactive - Code: {}, Status: {}", coupon.getCode(), coupon.getStatus());
            throw new IllegalArgsException("Coupon is inactive");
        }
        log.debug("Coupon status is ACTIVE");

        // Check date range
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartDate())) {
            log.warn("Coupon not active yet - Start Date: {}, Current: {}", coupon.getStartDate(), now);
            throw new IllegalArgsException("Coupon is not active yet");
        }
        log.debug("Coupon start date validation passed");

        if (now.isAfter(coupon.getExpiryDate())) {
            log.warn("Coupon expired - Expiry Date: {}, Current: {}", coupon.getExpiryDate(), now);
            throw new IllegalArgsException("Coupon has expired");
        }
        log.debug("Coupon expiry date validation passed");

        // Check usage limits
        if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
            log.warn("Coupon usage limit exceeded - Used: {}, Limit: {}",
                    coupon.getUsedCount(), coupon.getUsageLimit());
            throw new IllegalArgsException("Coupon usage limit exceeded");
        }
        log.debug("Coupon usage limit validation passed - Used: {}, Limit: {}",
                coupon.getUsedCount(), coupon.getUsageLimit());

        long userUsage = couponUsageRepository.countByCouponAndUser(coupon, user);
        if (userUsage >= coupon.getUsageLimitPerUser()) {
            log.warn("User coupon usage limit exceeded - User: {}, Used: {}, Limit: {}",
                    user.getId(), userUsage, coupon.getUsageLimitPerUser());
            throw new IllegalArgsException("Coupon usage limit exceeded for user");
        }
        log.debug("User coupon usage validation passed - Used: {}, Limit: {}",
                userUsage, coupon.getUsageLimitPerUser());

        // Check first order only
//        if (Boolean.TRUE.equals(coupon.getFirstOrderOnly())) {
//            long completedOrders = orderRepository.countByUserAndStatus(user, OrderStatus.DELIVERED);
//            if (completedOrders > 0) {
//                log.warn("Coupon only for first order - User: {}, Completed Orders: {}",
//                        user.getId(), completedOrders);
//                throw new IllegalArgsException("Coupon applicable only on first order");
//            }
//            log.debug("First order validation passed - Completed Orders: {}", completedOrders);
//        }

        log.debug("All coupon validations passed");
    }

    // ==================== DISCOUNT CALCULATION ====================

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        log.debug("Calculating discount - Coupon: {}, Amount: {}, Type: {}, Value: {}",
                coupon.getCode(), amount, coupon.getDiscountType(), coupon.getDiscountValue());

        if (coupon.getDiscountType() == DiscountType.FLAT) {
            BigDecimal discount = coupon.getDiscountValue();
            log.debug("Flat discount applied: {}", discount);
            return discount;
        }

        BigDecimal discount = amount.multiply(coupon.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        log.debug("Percentage discount calculated: {}", discount);

        if (coupon.getMaximumDiscount() != null) {
            BigDecimal maxDiscount = coupon.getMaximumDiscount();
            if (discount.compareTo(maxDiscount) > 0) {
                log.debug("Discount limited to max: {} (was: {})", maxDiscount, discount);
                discount = maxDiscount;
            }
        }

        log.info("Final discount calculated: {}", discount);
        return discount;
    }

    // ==================== GET COUPON ====================

    @Transactional(readOnly = true)
    public Coupon getCouponByCode(String code) {
        log.debug("Fetching coupon by code: {}", code);
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.error("Coupon not found with code: {}", code);
                    return new ResourceNotFoundException("Coupon not found");
                });
        log.debug("Coupon found - ID: {}, Code: {}, Status: {}",
                coupon.getId(), coupon.getCode(), coupon.getStatus());
        return coupon;
    }

    // ==================== CREATE COUPON ====================

    @Transactional
    @CacheEvict(value = "coupons", allEntries = true)
    public CouponResponse createCoupon(CouponCreateRequest request) {
        log.debug("=== CREATE COUPON STARTED ===");
        log.debug("Creating coupon - Code: {}, Discount Type: {}, Discount Value: {}",
                request.getCode(), request.getDiscountType(), request.getDiscountValue());

        if (couponRepository.existsByCode(request.getCode())) {
            log.warn("Coupon already exists with code: {}", request.getCode());
            throw new ConflictException("Coupon already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minimumOrderAmount(request.getMinimumOrderAmount())
                .maximumDiscount(request.getMaximumDiscount())
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .usageLimit(request.getUsageLimit())
                .usageLimitPerUser(request.getUsageLimitPerUser())
                .firstOrderOnly(request.getFirstOrderOnly())
                .status(CouponStatus.ACTIVE)
                .build();

        log.debug("Coupon entity built with ID: {}", coupon.getId());

        if (request.getSellerId() != null) {
            log.debug("Setting seller for coupon - Seller ID: {}", request.getSellerId());
            coupon.setSeller(userRepository.findById(request.getSellerId())
                    .orElseThrow(() -> {
                        log.error("Seller not found with ID: {}", request.getSellerId());
                        return new ResourceNotFoundException("Seller not found");
                    }));
            log.debug("Seller set successfully");
        }

        if (request.getCategoryId() != null) {
            log.debug("Setting category for coupon - Category ID: {}", request.getCategoryId());
            coupon.setCategory(categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> {
                        log.error("Category not found with ID: {}", request.getCategoryId());
                        return new ResourceNotFoundException("Category not found");
                    }));
            log.debug("Category set successfully");
        }

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon created successfully - ID: {}, Code: {}, Status: {}",
                saved.getId(), saved.getCode(), saved.getStatus());
        log.debug("=== CREATE COUPON COMPLETED ===");
        return couponMapper.mapToResponse(saved);
    }

    // ==================== GET COUPON BY ID ====================

    @Transactional(readOnly = true)
    @Cacheable(value = "coupon", key = "#couponId")
    public CouponResponse getCoupon(UUID couponId) {
        log.debug("Fetching coupon by ID: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> {
                    log.error("Coupon not found with ID: {}", couponId);
                    return new ResourceNotFoundException("Coupon not found");
                });

        log.debug("Coupon found - ID: {}, Code: {}, Status: {}, Used Count: {}",
                coupon.getId(), coupon.getCode(), coupon.getStatus(), coupon.getUsedCount());

        return couponMapper.mapToResponse(coupon);
    }

    // ==================== GET ALL COUPONS ====================

    @Transactional(readOnly = true)
    public Page<CouponResponse> getCoupons(int page, int size) {
        log.debug("Fetching all coupons - Page: {}, Size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Coupon> couponPage = couponRepository.findAll(pageable);

        log.info("Retrieved {} coupons - Page: {}, Total Elements: {}, Total Pages: {}",
                couponPage.getNumberOfElements(), page, couponPage.getTotalElements(), couponPage.getTotalPages());

        if (couponPage.getContent().isEmpty()) {
            log.warn("No coupons found");
        }

        return couponPage.map(couponMapper::mapToResponse);
    }

    // ==================== DELETE COUPON ====================

    @Transactional
    @CacheEvict(value = {"coupon", "coupons"}, allEntries = true)
    public void deleteCoupon(UUID couponId) {
        log.debug("=== DELETE COUPON STARTED ===");
        log.warn("Deleting coupon with ID: {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> {
                    log.error("Coupon not found with ID: {}", couponId);
                    return new ResourceNotFoundException("Coupon not found");
                });

        log.debug("Coupon found - Code: {}, Status: {}", coupon.getCode(), coupon.getStatus());

        couponRepository.delete(coupon);
        log.info("Coupon deleted successfully - ID: {}, Code: {}", couponId, coupon.getCode());
        log.debug("=== DELETE COUPON COMPLETED ===");
    }

    // ==================== UPDATE COUPON ====================

    @Transactional
    @CachePut(value = "coupon", key = "#couponId")
    @CacheEvict(value = "coupons", allEntries = true)
    public CouponResponse updateCoupon(UUID couponId, CouponUpdateRequest request) {
        log.debug("=== UPDATE COUPON STARTED ===");
        log.debug("Updating coupon - ID: {}, Request: {}", couponId, request);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> {
                    log.error("Coupon not found with ID: {}", couponId);
                    return new ResourceNotFoundException("Coupon not found");
                });

        log.debug("Existing coupon found - Code: {}, Status: {}", coupon.getCode(), coupon.getStatus());
        int updateCount = 0;

        if (request.getTitle() != null) {
            coupon.setTitle(request.getTitle());
            updateCount++;
            log.debug("Updated title to: {}", request.getTitle());
        }

        if (request.getDescription() != null) {
            coupon.setDescription(request.getDescription());
            updateCount++;
            log.debug("Updated description");
        }

        if (request.getDiscountType() != null) {
            coupon.setDiscountType(request.getDiscountType());
            updateCount++;
            log.debug("Updated discount type to: {}", request.getDiscountType());
        }

        if (request.getDiscountValue() != null) {
            coupon.setDiscountValue(request.getDiscountValue());
            updateCount++;
            log.debug("Updated discount value to: {}", request.getDiscountValue());
        }

        if (request.getMinimumOrderAmount() != null) {
            coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
            updateCount++;
            log.debug("Updated minimum order amount to: {}", request.getMinimumOrderAmount());
        }

        if (request.getMaximumDiscount() != null) {
            coupon.setMaximumDiscount(request.getMaximumDiscount());
            updateCount++;
            log.debug("Updated maximum discount to: {}", request.getMaximumDiscount());
        }

        if (request.getStartDate() != null) {
            coupon.setStartDate(request.getStartDate());
            updateCount++;
            log.debug("Updated start date to: {}", request.getStartDate());
        }

        if (request.getExpiryDate() != null) {
            coupon.setExpiryDate(request.getExpiryDate());
            updateCount++;
            log.debug("Updated expiry date to: {}", request.getExpiryDate());
        }

        if (request.getUsageLimit() != null) {
            coupon.setUsageLimit(request.getUsageLimit());
            updateCount++;
            log.debug("Updated usage limit to: {}", request.getUsageLimit());
        }

        if (request.getUsageLimitPerUser() != null) {
            coupon.setUsageLimitPerUser(request.getUsageLimitPerUser());
            updateCount++;
            log.debug("Updated usage limit per user to: {}", request.getUsageLimitPerUser());
        }

        if (request.getStatus() != null) {
            coupon.setStatus(request.getStatus());
            updateCount++;
            log.debug("Updated status to: {}", request.getStatus());
        }

        if (request.getFirstOrderOnly() != null) {
            coupon.setFirstOrderOnly(request.getFirstOrderOnly());
            updateCount++;
            log.debug("Updated first order only to: {}", request.getFirstOrderOnly());
        }

        log.info("Applied {} updates to coupon - ID: {}, Code: {}", updateCount, couponId, coupon.getCode());

        Coupon saved = couponRepository.save(coupon);
        log.debug("Coupon saved successfully - ID: {}, Code: {}", saved.getId(), saved.getCode());
        log.debug("=== UPDATE COUPON COMPLETED ===");
        return couponMapper.mapToResponse(saved);
    }

    // ==================== ACTIVATE/DEACTIVATE COUPON ====================

    @Transactional
    public void activateCoupon(UUID id) {
        log.debug("=== ACTIVATE COUPON STARTED ===");
        log.info("Activating coupon with ID: {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Coupon not found with ID: {}", id);
                    return new ResourceNotFoundException("Coupon not found");
                });

        if (coupon.getStatus() == CouponStatus.ACTIVE) {
            log.warn("Coupon is already active - ID: {}, Code: {}", id, coupon.getCode());
            return;
        }

        coupon.setStatus(CouponStatus.ACTIVE);
        couponRepository.save(coupon);
        log.info("Coupon activated successfully - ID: {}, Code: {}", id, coupon.getCode());
        log.debug("=== ACTIVATE COUPON COMPLETED ===");
    }

    @Transactional
    public void deactivateCoupon(UUID id) {
        log.debug("=== DEACTIVATE COUPON STARTED ===");
        log.info("Deactivating coupon with ID: {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Coupon not found with ID: {}", id);
                    return new ResourceNotFoundException("Coupon not found");
                });

        if (coupon.getStatus() == CouponStatus.INACTIVE) {
            log.warn("Coupon is already inactive - ID: {}, Code: {}", id, coupon.getCode());
            return;
        }

        coupon.setStatus(CouponStatus.INACTIVE);
        couponRepository.save(coupon);
        log.info("Coupon deactivated successfully - ID: {}, Code: {}", id, coupon.getCode());
        log.debug("=== DEACTIVATE COUPON COMPLETED ===");
    }


    private List<EligibleItem> resolveEligibleItems(CouponValidationContext context) {
        log.debug("Resolving eligible items for coupon application");
        List<EligibleItem> items = new ArrayList<>();

        if (context.getCart() != null) {
            log.debug("Processing cart items - Cart ID: {}, Items: {}",
                    context.getCart().getId(), context.getCart().getCartItems().size());

            for (CartItem cartItem : context.getCart().getCartItems()) {
                ProductVariant variant = cartItem.getProductVariant();
                BigDecimal amount = variant.getSellingPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

                items.add(EligibleItem.builder()
                        .productVariant(variant)
                        .quantity(cartItem.getQuantity())
                        .amount(amount)
                        .build());

                log.trace("Added cart item - Variant: {}, Quantity: {}, Amount: {}",
                        variant.getId(), cartItem.getQuantity(), amount);
            }
        } else {
            log.debug("Processing buy now item - Variant: {}, Quantity: {}",
                    context.getBuyNowVariant().getId(), context.getBuyNowQuantity());

            ProductVariant variant = context.getBuyNowVariant();
            BigDecimal amount = variant.getSellingPrice()
                    .multiply(BigDecimal.valueOf(context.getBuyNowQuantity()));

            items.add(EligibleItem.builder()
                    .productVariant(variant)
                    .quantity(context.getBuyNowQuantity())
                    .amount(amount)
                    .build());

            log.trace("Added buy now item - Variant: {}, Quantity: {}, Amount: {}",
                    variant.getId(), context.getBuyNowQuantity(), amount);
        }

        List<EligibleItem> filteredItems = filterEligibleItems(items, context.getCoupon());
        log.debug("Eligible items resolved - Original: {}, Filtered: {}",
                items.size(), filteredItems.size());

        return filteredItems;
    }

    private List<EligibleItem> filterEligibleItems(List<EligibleItem> items, Coupon coupon) {
        log.debug("Filtering eligible items - Coupon: {}, Total Items: {}",
                coupon.getCode(), items.size());

        Stream<EligibleItem> stream = items.stream();

        if (coupon.getSeller() != null) {
            UUID sellerId = coupon.getSeller().getId();
            log.debug("Filtering by seller - ID: {}", sellerId);
            stream = stream.filter(item ->
                    item.getProductVariant()
                            .getProduct()
                            .getSeller()
                            .getId()
                            .equals(sellerId));
        }

        if (coupon.getCategory() != null) {
            UUID categoryId = coupon.getCategory().getId();
            log.debug("Filtering by category - ID: {}", categoryId);
            stream = stream.filter(item ->
                    item.getProductVariant()
                            .getProduct()
                            .getCategory()
                            .getId()
                            .equals(categoryId));
        }

        List<EligibleItem> result = stream.toList();
        log.debug("Filtering completed - Remaining items: {}", result.size());
        return result;
    }

    private BigDecimal calculateEligibleAmount(List<EligibleItem> items) {
        BigDecimal total = items.stream()
                .map(EligibleItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Eligible amount calculated - Total: {}, Items: {}", total, items.size());
        return total;
    }
}