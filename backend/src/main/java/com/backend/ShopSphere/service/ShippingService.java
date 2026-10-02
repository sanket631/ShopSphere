package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.ShippingResponse;
import com.backend.ShopSphere.enums.ShippingMethod;
import com.backend.ShopSphere.exception.IllegalArgsException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    @Value("${shopsphere.shipping.standard.charge}")
    private BigDecimal standardShippingCharge;

    @Value("${shopsphere.shipping.express.charge}")
    private BigDecimal expressShippingCharge;

    @Value("${shopsphere.shipping.same-day.charge}")
    private BigDecimal sameDayDeliveryShippingCharge;

    @Value("${shopsphere.shipping.free-threshold}")
    private BigDecimal freeShippingThresholdAmount;

    @Value("${shopsphere.shipping.standard.days}")
    private long standardShippingDays;

    @Value("${shopsphere.shipping.express.days}")
    private long expressShippingDays;

    @Value("${shopsphere.shipping.same-day.days}")
    private long sameDayShippingDays;

    public ShippingResponse calculateShipping(BigDecimal orderAmount, ShippingMethod shippingMethod) {
        log.debug("=== CALCULATE SHIPPING STARTED ===");
        log.debug("Order Amount: {}, Shipping Method: {}", orderAmount, shippingMethod);

        if (orderAmount == null || orderAmount.signum() < 0) {
            log.error("Invalid order amount: {}", orderAmount);
            throw new IllegalArgsException("Order amount cannot be null or negative");
        }
        log.debug("Order amount validation passed");

        if (shippingMethod == null) {
            log.error("Shipping method is null");
            throw new IllegalArgsException("Shipping method is required");
        }
        log.debug("Shipping method validation passed");

        ShippingResponse response = switch (shippingMethod) {
            case STANDARD -> {
                log.debug("Calculating STANDARD shipping");
                yield calculateStandardShipping(orderAmount);
            }
            case EXPRESS -> {
                log.debug("Calculating EXPRESS shipping - Charge: {}, Days: {}",
                        expressShippingCharge, expressShippingDays);
                yield ShippingResponse.builder()
                        .shippingMethod(ShippingMethod.EXPRESS)
                        .shippingCharge(expressShippingCharge)
                        .freeShipping(false)
                        .estimatedDelivery(LocalDate.now().plusDays(expressShippingDays))
                        .build();
            }
            case SAME_DAY -> {
                log.debug("Calculating SAME_DAY shipping - Charge: {}, Days: {}",
                        sameDayDeliveryShippingCharge, sameDayShippingDays);
                yield ShippingResponse.builder()
                        .shippingMethod(ShippingMethod.SAME_DAY)
                        .shippingCharge(sameDayDeliveryShippingCharge)
                        .freeShipping(false)
                        .estimatedDelivery(LocalDate.now().plusDays(sameDayShippingDays))
                        .build();
            }
        };

        log.info("Shipping calculation completed - Method: {}, Charge: {}, Delivery: {}",
                response.getShippingMethod(), response.getShippingCharge(),
                response.getEstimatedDelivery());
        log.debug("=== CALCULATE SHIPPING COMPLETED ===");
        return response;
    }

    private ShippingResponse calculateStandardShipping(BigDecimal orderAmount) {
        log.debug("=== CALCULATE STANDARD SHIPPING STARTED ===");
        log.debug("Order Amount: {}, Free Shipping Threshold: {}",
                orderAmount, freeShippingThresholdAmount);

        boolean freeShipping = orderAmount.compareTo(freeShippingThresholdAmount) >= 0;
        BigDecimal shippingCharge = freeShipping ? BigDecimal.ZERO : standardShippingCharge;

        log.info("Standard shipping calculated - Order: {}, Free Shipping: {}, Charge: {}, Delivery: {} days",
                orderAmount, freeShipping, shippingCharge, standardShippingDays);

        ShippingResponse response = ShippingResponse.builder()
                .shippingMethod(ShippingMethod.STANDARD)
                .shippingCharge(shippingCharge)
                .freeShipping(freeShipping)
                .estimatedDelivery(LocalDate.now().plusDays(standardShippingDays))
                .build();

        log.debug("=== CALCULATE STANDARD SHIPPING COMPLETED ===");
        return response;
    }
}