package com.backend.ShopSphere.service;

import com.backend.ShopSphere.dto.PaymentGatewayResponse;
import com.backend.ShopSphere.dto.PaymentResponse;
import com.backend.ShopSphere.entity.*;
import com.backend.ShopSphere.enums.OrderStatus;
import com.backend.ShopSphere.enums.PaymentStatus;
import com.backend.ShopSphere.events.PaymentFailedEvent;
import com.backend.ShopSphere.events.PaymentSuccessEvent;
import com.backend.ShopSphere.exception.ConflictException;
import com.backend.ShopSphere.exception.IllegalArgsException;
import com.backend.ShopSphere.exception.ResourceNotFoundException;
import com.backend.ShopSphere.repository.OrderRepository;
import com.backend.ShopSphere.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    @Transactional
    public PaymentResponse processPayment(UUID orderId){
        log.debug("Request received to process payment for orderId={}", orderId);

        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> {
                    log.error("Order not found with orderId={}", orderId);
                    throw new ResourceNotFoundException("Order not found");
                }
        );
        log.debug("Order found successfully for orderId={}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Payment not found for the order: " + orderId)
        );

        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            throw new ConflictException("Payment already completed");
        }

        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            throw new ConflictException(
                    "Payment cannot be processed in status: "
                            + payment.getStatus()
            );
        }

        BigDecimal amount = order.getFinalAmount();
        PaymentGatewayResponse gatewayResponse = paymentGateway.processPayment(orderId, amount);

        PaymentStatus status = payment.getStatus();
        log.debug("Current payment status for orderId={}: {}", orderId, status);

        if(PaymentStatus.SUCCESS.equals(status)){
            log.warn("Payment already processed for orderId={}. Current status: {}", orderId, status);
            throw new IllegalArgsException("Payment is already made for the order.");
        }

        if (gatewayResponse.isSuccessful()) {

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(
                    gatewayResponse.getTransactionId()
            );
            payment.setPaidAt(LocalDateTime.now());

            paymentRepository.save(payment);

            orderLifecycleService.transition(order, OrderStatus.CONFIRMED);

            PaymentSuccessEvent event = new PaymentSuccessEvent(
                    order.getId(),
                    order.getUser().getId(),
                    payment.getId()
            );
            eventPublisher.publishEvent(event);

            log.info(
                    "Payment successful. orderId={}, paymentId={}, amount={}, transactionId={}",
                    orderId,
                    payment.getId(),
                    amount,
                    payment.getTransactionId()
            );

        } else {

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            PaymentFailedEvent event = new PaymentFailedEvent(
                    order.getId(),
                    order.getUser().getId(),
                    payment.getId(),
                    null
            );

            log.warn(
                    "Payment failed. orderId={}, paymentId={}, amount={}",
                    orderId,
                    payment.getId(),
                    amount
            );
        }

        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .failedAt(payment.getFailedAt())
                .build();
    }
}