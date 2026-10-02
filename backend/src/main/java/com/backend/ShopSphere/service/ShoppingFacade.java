package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.entity.*;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import com.backend.ShopSphere.enums.OrderStatus;
import com.backend.ShopSphere.enums.PaymentStatus;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.exception.UnauthorizedException;
import com.backend.ShopSphere.repository.*;
import com.backend.ShopSphere.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingFacade {

    private final CartService cartService;
    private final WishListItemService wishListItemService;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CheckoutPricingService checkoutPricingService;

    @Value("${shopsphere.order.reservation-expiry-minutes}")
    private int reservationExpiryMinutes;

    @Transactional
    public CartResponse moveWishlistToCart(UUID variantId) {

        wishListItemService.removeWishListItem(variantId);

        return cartService.addToCart(new AddToCartRequest(variantId, 1));
    }

    @Transactional
    public void moveCartToWishlist(UUID cartItemId) {

        CartItem cartItem = cartService.getCartItem(cartItemId);

        wishListItemService.addWishListItem(
                cartItem.getProductVariant().getId()
        );
        cartService.removeCartItem(cartItemId);
    }

    @Transactional
    public OrderResponse buyNow(BuyNowRequest request) {
        String userEmail = securityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(userEmail).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        ProductVariant productVariant = productVariantRepository.findById(request.getProductId()).orElseThrow(
                () -> new ResourceNotFoundException("Product not found")
        );

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(
                        ()->new ResourceNotFoundException("Address not found")
                );
        if(!address.getUser().getId().equals(user.getId())){
            throw new UnauthorizedException("Address does not belong to current user");
        }


        CheckoutPriceResult priceResult = checkoutPricingService.calculateBuyNowPrice(productVariant, request.getQuantity(), user, request.getCouponCode(), request.getShippingMethod());
        log.info("Buy-now pricing calculated. variantId={}, originalAmount={}, discount={}, shipping={}, finalAmount={}",
                productVariant.getId(),
                priceResult.getOriginalAmount(),
                priceResult.getDiscountAmount(),
                priceResult.getShippingCharge(),
                priceResult.getFinalAmount());

        log.info("Stock reserved for buy-now. variantId={}, quantity={}",
                productVariant.getId(), request.getQuantity());

        inventoryService.reserveStock(productVariant, request.getQuantity());

        Order order = Order.builder()
                .user(user)
                .totalAmount(priceResult.getOriginalAmount())
                .discountAmount(priceResult.getDiscountAmount())
                .finalAmount(priceResult.getFinalAmount())
                .coupon(priceResult.getCoupon())
                .status(OrderStatus.PENDING_CONFIRMATION)
                .shippingMethod(priceResult.getShipping().getShippingMethod())
                .shippingCharge(priceResult.getShippingCharge())
                .estimatedDeliveryDate(priceResult.getShipping().getEstimatedDelivery())
                .reservationExpiry(LocalDateTime.now().plusMinutes(reservationExpiryMinutes))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .sku(productVariant.getSku())
                .productPrice(productVariant.getSellingPrice())
                .status(FulfillmentStatus.PENDING_SELLER_CONFIRMATION)
                .productVariantId(productVariant.getId())
                .quantity(request.getQuantity())
                .attributes(productVariant.getAttributes())
                .subtotal(priceResult.getOriginalAmount())
                .isReturnApplicable(productVariant.getProduct().getReturnApplicable())
                .returnWindowDays(productVariant.getProduct().getReturnWindowDays())
                .build();
        List<OrderItem> orderItemList = new ArrayList<>();
        orderItemList.add(orderItem);

        order.setOrderItems(orderItemList);


        copyShippingAddress(order, address);

        Order saved = orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(saved)
                .amount(saved.getFinalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);
        saved.setPayment(payment);

        return mapToOrderResponse(saved);
    }

    private void copyShippingAddress(Order order, Address address){

        order.setShippingFullName(address.getFullName());
        order.setShippingPhoneNumber(address.getPhoneNumber());
        order.setShippingAddressLine1(address.getAddressLine1());
        order.setShippingAddressLine2(address.getAddressLine2());
        order.setShippingLandmark(address.getLandmark());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem){
        return OrderItemResponse.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProductVariantId())
                .productName(orderItem.getSku())
                .status(FulfillmentStatus.PENDING_SELLER_CONFIRMATION)
                .productPrice(orderItem.getProductPrice())
                .quantity(orderItem.getQuantity())
                .subtotal(orderItem.getSubtotal())
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order){
        List<OrderItemResponse> orderItemResponseList = order.getOrderItems()
                .stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());
        PaymentStatus paymentStatus = order.getPayment().getStatus();
        return OrderResponse.builder()
                .orderId(order.getId())
                .items(orderItemResponseList)
                .status(order.getStatus())
                .paymentStatus(paymentStatus)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingCharge(order.getShippingCharge())
                .otherCharges(order.getOtherCharges())
                .finalAmount(order.getFinalAmount())
                .couponCode(order.getCoupon() != null ? order.getCoupon().getCode(): "")
                .shippingAddress(mapShippingAddress(order))
                .build();
    }

    private ShippingAddressResponse mapShippingAddress(Order order) {
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