package com.backend.ShopSphere.service;

import com.backend.ShopSphere.constant.EntityType;
import com.backend.ShopSphere.dto.*;
import com.backend.ShopSphere.entity.*;
import com.backend.ShopSphere.enums.FulfillmentStatus;
import com.backend.ShopSphere.enums.OrderStatus;
import com.backend.ShopSphere.enums.PaymentStatus;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.exception.UnauthorizedException;
import com.backend.ShopSphere.repository.*;
import com.backend.ShopSphere.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final AddressRepository addressRepository;
    private final CheckoutPricingService checkoutPricingService;
    private final AuditService auditService;

    @Value("${shopsphere.order.reservation-expiry-minutes}")
    private int reservationExpiryMinutes;

    // ==================== CHECKOUT ====================

    @Transactional
    public OrderResponse checkoutCart(CheckoutOrderRequest request) {
        log.debug("=== CHECKOUT CART STARTED ===");
        log.debug("Checkout request: {}", request);

        String currentUserEmail = securityUtil.getCurrentUserEmail();
        log.debug("Extracted user email: {}", currentUserEmail);

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> {
                    log.error("User not found during checkout - Email: {}", currentUserEmail);
                    return new ResourceNotFoundException("User not found");
                });
        log.debug("User found - ID: {}, Email: {}", user.getId(), user.getEmail());

        Cart cart = user.getCart();
        if (cart == null) {
            log.error("No cart found for user - User ID: {}", user.getId());
            throw new ResourceNotFoundException("No cart found for the user");
        }
        log.debug("Cart found - ID: {}, Items Count: {}", cart.getId(),
                cart.getCartItems() != null ? cart.getCartItems().size() : 0);

        // Validate address
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> {
                    log.error("Address not found - Address ID: {}", request.getAddressId());
                    return new ResourceNotFoundException("Address not found");
                });
        log.debug("Address found - ID: {}, City: {}", address.getId(), address.getCity());

        if (!address.getUser().getId().equals(user.getId())) {
            log.warn("Address does not belong to user - Address User: {}, Current User: {}",
                    address.getUser().getId(), user.getId());
            throw new UnauthorizedException("Address does not belong to current user");
        }
        log.debug("Address ownership validated");

        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems == null || cartItems.isEmpty()) {
            log.warn("User cart is empty - User ID: {}", user.getId());
            throw new IllegalArgsException("Cart is empty");
        }
        log.info("User cart contains {} items", cartItems.size());

        // Map cart items to order items
        log.debug("Mapping cart items to order items");
        List<OrderItem> orderItemList = cartItems.stream()
                .map(this::mapToOrderItem)
                .collect(Collectors.toList());
        log.debug("Mapped {} order items", orderItemList.size());

        // Calculate pricing
        log.debug("Calculating cart pricing");
        CheckoutPriceResult pricing = checkoutPricingService.calculateCartPrice(
                cart,
                user,
                request.getCouponCode(),
                request.getShippingMethod()
        );
        log.info("Pricing calculated - Original: {}, Discount: {}, Shipping: {}, Final: {}",
                pricing.getOriginalAmount(), pricing.getDiscountAmount(),
                pricing.getShippingCharge(), pricing.getFinalAmount());

        // Reserve inventory
        log.debug("Reserving inventory for cart items");
        inventoryService.reserveStock(cart);
        log.info("Inventory reserved successfully");

        // Build order
        Order order = Order.builder()
                .user(user)
                .totalAmount(pricing.getOriginalAmount())
                .discountAmount(pricing.getDiscountAmount())
                .shippingCharge(pricing.getShippingCharge())
                .finalAmount(pricing.getFinalAmount())
                .coupon(pricing.getCoupon())
                .status(OrderStatus.PENDING_CONFIRMATION)
                .shippingMethod(pricing.getShipping().getShippingMethod())
                .estimatedDeliveryDate(pricing.getShipping().getEstimatedDelivery())
                .reservationExpiry(LocalDateTime.now().plusMinutes(reservationExpiryMinutes))
                .build();
        log.debug("Order entity built - Status: {}, Reservation Expiry: {}",
                order.getStatus(), order.getReservationExpiry());

        // Set order items
        orderItemList.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItemList);
        log.debug("Order items set - Count: {}", orderItemList.size());

        // Copy shipping address
        copyShippingAddress(order, address);
        log.debug("Shipping address copied to order");

        // Save order
        Order saved = orderRepository.save(order);
        log.info("Order created successfully - Order ID: {}, User ID: {}, Final Amount: {}",
                saved.getId(), saved.getUser().getId(), saved.getFinalAmount());
        //audit log
        UUID currentUserId = securityUtil.getCurrentUserId();
        String currentUserRole = securityUtil.getCurrentUserRole();

        if (currentUserId == null || currentUserRole == null) {
            throw new IllegalStateException(
                    "Unable to determine current user identity"
            );
        }

        for (OrderItem orderItem : saved.getOrderItems()) {

            AuditRequest auditRequest = new AuditRequest(
                    EntityType.ORDER_ITEM,
                    orderItem.getId(),
                    null,
                    FulfillmentStatus.PENDING_SELLER_CONFIRMATION.name(),
                    "ORDER_ITEM_CREATED",
                    currentUserId,
                    currentUserRole,
                    "Order item created during checkout"
            );

            auditService.recordStatusChange(auditRequest);
        }

        // Create payment
        Payment payment = Payment.builder()
                .order(saved)
                .amount(saved.getFinalAmount())
                .status(PaymentStatus.PENDING)
                .build();
        log.debug("Payment entity created - Amount: {}, Status: {}",
                payment.getAmount(), payment.getStatus());

        paymentRepository.save(payment);
        saved.setPayment(payment);
        log.info("Payment created for order - Order ID: {}, Payment ID: {}",
                saved.getId(), payment.getId());

        log.debug("=== CHECKOUT CART COMPLETED ===");
        return mapToOrderResponse(saved);
    }

    // ==================== GET ORDER ====================

    public OrderResponse getOrder(UUID id) {
        log.debug("=== GET ORDER STARTED ===");
        log.debug("Fetching order with ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found - Order ID: {}", id);
                    return new ResourceNotFoundException("Order not found");
                });
        log.debug("Order found - ID: {}, Status: {}, User: {}",
                order.getId(), order.getStatus(), order.getUser().getEmail());

        // Validate ownership
        securityUtil.validateCurrentUser(order.getUser().getEmail());
        log.debug("User authorization validated");

        log.debug("=== GET ORDER COMPLETED ===");
        return mapToOrderResponse(order);
    }

    // ==================== GET ALL ORDERS ====================

    public List<OrderResponse> getAllOrders() {
        log.debug("=== GET ALL ORDERS STARTED ===");

        String currentUserEmail = securityUtil.getCurrentUserEmail();
        log.info("Extracted user email: {}", currentUserEmail);

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> {
                    log.error("User not found - Email: {}", currentUserEmail);
                    return new ResourceNotFoundException("User not found");
                });
        log.debug("User found - ID: {}, Email: {}", user.getId(), user.getEmail());

        List<Order> orders = user.getOrders();
        log.info("Retrieved {} orders for user - User ID: {}",
                orders != null ? orders.size() : 0, user.getId());

        if (orders == null || orders.isEmpty()) {
            log.warn("No orders found for user - User ID: {}", user.getId());
        } else {
            log.debug("Order list summary:");
            orders.forEach(order -> log.debug("  - Order ID: {}, Status: {}, Final Amount: {}",
                    order.getId(), order.getStatus(), order.getFinalAmount()));
        }

        List<OrderResponse> responses = orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());

        log.info("Returning {} order responses", responses.size());
        log.debug("=== GET ALL ORDERS COMPLETED ===");
        return responses;
    }

    // ==================== PRIVATE HELPERS ====================

    private void copyShippingAddress(Order order, Address address) {
        log.debug("Copying shipping address from address ID: {} to order", address.getId());

        order.setShippingFullName(address.getFullName());
        order.setShippingPhoneNumber(address.getPhoneNumber());
        order.setShippingAddressLine1(address.getAddressLine1());
        order.setShippingAddressLine2(address.getAddressLine2());
        order.setShippingLandmark(address.getLandmark());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());

        log.trace("Shipping address copied - Name: {}, City: {}, State: {}, Country: {}",
                address.getFullName(), address.getCity(), address.getState(), address.getCountry());
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

    public OrderItem mapToOrderItem(CartItem cartItem) {
        ProductVariant product = cartItem.getProductVariant();
        log.trace("Mapping cart item to order item - Cart Item ID: {}, Variant ID: {}, Quantity: {}",
                cartItem.getId(), product.getId(), cartItem.getQuantity());

        OrderItem orderItem = OrderItem.builder()
                .productVariantId(product.getId())
                .sku(product.getSku())
                .status(FulfillmentStatus.PENDING_SELLER_CONFIRMATION)
                .productPrice(product.getSellingPrice())
                .quantity(cartItem.getQuantity())
                .attributes(product.getAttributes())
                .subtotal(product.getSellingPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .isReturnApplicable(product.getProduct().getReturnApplicable())
                .returnWindowDays(product.getProduct().getReturnWindowDays())
                .build();

        log.trace("Order item mapped - SKU: {}, Subtotal: {}",
                orderItem.getSku(), orderItem.getSubtotal());
        return orderItem;
    }

    public OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        log.trace("Mapping order item to response - Order Item ID: {}", orderItem.getId());

        OrderItemResponse response = OrderItemResponse.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProductVariantId())
                .productName(orderItem.getSku())
                .productPrice(orderItem.getProductPrice())
                .quantity(orderItem.getQuantity())
                .subtotal(orderItem.getSubtotal())
                .status(orderItem.getStatus())
                .build();

        log.trace("Order item response mapped - Product: {}, Quantity: {}, Subtotal: {}",
                response.getProductName(), response.getQuantity(), response.getSubtotal());
        return response;
    }

    public OrderResponse mapToOrderResponse(Order order) {
        log.trace("Mapping order to response - Order ID: {}, Status: {}",
                order.getId(), order.getStatus());

        List<OrderItemResponse> orderItemResponseList = order.getOrderItems()
                .stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        PaymentStatus paymentStatus = order.getPayment() != null ?
                order.getPayment().getStatus() : null;

        OrderResponse response = OrderResponse.builder()
                .orderId(order.getId())
                .items(orderItemResponseList)
                .status(order.getStatus())
                .paymentStatus(paymentStatus)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingCharge(order.getShippingCharge())
                .otherCharges(order.getOtherCharges())
                .finalAmount(order.getFinalAmount())
                .couponCode(order.getCoupon() != null ? order.getCoupon().getCode() : "")
                .shippingAddress(mapShippingAddress(order))
                .build();

        log.trace("Order response mapped - Order ID: {}, Items: {}, Final Amount: {}",
                response.getOrderId(), response.getItems().size(), response.getFinalAmount());
        return response;
    }
}