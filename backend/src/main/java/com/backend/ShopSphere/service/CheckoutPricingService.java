package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.ApplyCouponResponse;
import com.backend.ShopSphere.dto.CheckoutPriceResult;
import com.backend.ShopSphere.dto.CouponValidationContext;
import com.backend.ShopSphere.dto.ShippingResponse;
import com.backend.ShopSphere.entity.Cart;
import com.backend.ShopSphere.entity.Coupon;
import com.backend.ShopSphere.entity.ProductVariant;
import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.enums.ShippingMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutPricingService {

    private final CouponService couponService;
    private final ShippingService shippingService;

    @Transactional(readOnly = true)
    public CheckoutPriceResult calculateCartPrice(
            Cart cart,
            User user,
            String couponCode,
            ShippingMethod shippingMethod) {

        log.debug("=== CALCULATE CART PRICE STARTED ===");
        log.debug("User ID: {}, Coupon Code: {}, Shipping Method: {}",
                user.getId(), couponCode, shippingMethod);
        log.debug("Cart ID: {}, Cart Items Count: {}",
                cart.getId(), cart.getCartItems() != null ? cart.getCartItems().size() : 0);

        // Calculate original amount from cart items
        BigDecimal originalAmount = cart.getCartItems()
                .stream()
                .map(item -> {
                    BigDecimal price = item.getProductVariant().getSellingPrice();
                    BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
                    BigDecimal subtotal = price.multiply(quantity);
                    log.trace("Cart item - Variant ID: {}, Price: {}, Quantity: {}, Subtotal: {}",
                            item.getProductVariant().getId(), price, item.getQuantity(), subtotal);
                    return subtotal;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Cart total calculated - Original Amount: {}", originalAmount);

        CheckoutPriceResult result = calculate(
                originalAmount,
                couponCode,
                shippingMethod,
                CouponValidationContext.builder()
                        .cart(cart)
                        .user(user)
                        .totalOrderAmount(originalAmount)
                        .build()
        );

        log.info("Cart price calculation completed - Final Amount: {}", result.getFinalAmount());
        log.debug("=== CALCULATE CART PRICE COMPLETED ===");
        return result;
    }

    @Transactional(readOnly = true)
    public CheckoutPriceResult calculateBuyNowPrice(
            ProductVariant productVariant,
            Integer quantity,
            User user,
            String couponCode,
            ShippingMethod shippingMethod) {

        log.debug("=== CALCULATE BUY NOW PRICE STARTED ===");
        log.debug("User ID: {}, Variant ID: {}, Quantity: {}, Coupon: {}, Shipping: {}",
                user.getId(), productVariant.getId(), quantity, couponCode, shippingMethod);

        BigDecimal originalAmount = productVariant.getSellingPrice().multiply(BigDecimal.valueOf(quantity));
        log.info("Buy now total calculated - Original Amount: {}, Unit Price: {}, Quantity: {}",
                originalAmount, productVariant.getSellingPrice(), quantity);

        CheckoutPriceResult result = calculate(
                originalAmount,
                couponCode,
                shippingMethod,
                CouponValidationContext.builder()
                        .user(user)
                        .buyNowVariant(productVariant)
                        .buyNowQuantity(quantity)
                        .totalOrderAmount(originalAmount)
                        .build()
        );

        log.info("Buy now price calculation completed - Final Amount: {}", result.getFinalAmount());
        log.debug("=== CALCULATE BUY NOW PRICE COMPLETED ===");
        return result;
    }

    private CheckoutPriceResult calculate(
            BigDecimal originalAmount,
            String couponCode,
            ShippingMethod shippingMethod,
            CouponValidationContext context) {

        log.debug("=== PRICE CALCULATION STARTED ===");
        log.debug("Original Amount: {}, Coupon Code: {}, Shipping Method: {}",
                originalAmount, couponCode, shippingMethod);

        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (StringUtils.hasText(couponCode)) {
            log.debug("Coupon code provided: {}. Attempting to apply coupon...", couponCode);
            coupon = couponService.getCouponByCode(couponCode);
            context.setCoupon(coupon);
            log.debug("Coupon found - ID: {}, Code: {}, Discount Type: {}, Discount Value: {}",
                    coupon.getId(), coupon.getCode(), coupon.getDiscountType(), coupon.getDiscountValue());

            ApplyCouponResponse couponResponse = couponService.applyCoupon(context);
            discount = couponResponse.getDiscountAmount();
            log.info("Coupon applied successfully - Discount Amount: {}, Eligible Amount: {}, Final Amount: {}",
                    discount, couponResponse.getEligibleAmount(), couponResponse.getFinalAmount());
        } else {
            log.debug("No coupon code provided. Skipping coupon application.");
        }

        ShippingResponse shipping = shippingService.calculateShipping(originalAmount, shippingMethod);
        log.debug("Shipping calculated - Method: {}, Charge: {}, Estimated Delivery: {}",
                shipping.getShippingMethod(), shipping.getShippingCharge(),
                shipping.getEstimatedDelivery());

        BigDecimal finalAmount = originalAmount.subtract(discount).add(shipping.getShippingCharge());
        log.debug("Final amount before adjustment: {}", finalAmount);

        if (finalAmount.signum() < 0) {
            finalAmount = BigDecimal.ZERO;
            log.warn("Final amount was negative, set to zero");
        }

        log.info(
                "Checkout price calculation completed - Original: {}, Discount: {}, Shipping: {}, Final: {}",
                originalAmount, discount, shipping.getShippingCharge(), finalAmount
        );

        CheckoutPriceResult result = CheckoutPriceResult.builder()
                .originalAmount(originalAmount)
                .discountAmount(discount)
                .shippingCharge(shipping.getShippingCharge())
                .finalAmount(finalAmount)
                .coupon(coupon)
                .shipping(shipping)
                .build();

        log.debug("=== PRICE CALCULATION COMPLETED ===");
        return result;
    }
}