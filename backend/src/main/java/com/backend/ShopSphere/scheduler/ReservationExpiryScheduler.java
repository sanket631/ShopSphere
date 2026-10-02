package com.backend.ShopSphere.scheduler;

import com.backend.ShopSphere.entity.Order;
import com.backend.ShopSphere.enums.OrderStatus;
import com.backend.ShopSphere.enums.PaymentStatus;
import com.backend.ShopSphere.events.ReservationExpiredEvent;
import com.backend.ShopSphere.repository.OrderRepository;
import com.backend.ShopSphere.repository.PaymentRepository;
import com.backend.ShopSphere.service.EventPublisher;
import com.backend.ShopSphere.service.OrderLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final EventPublisher eventPublisher;

    @Transactional
    @Scheduled(fixedDelayString = "${shopsphere.scheduler.reservation-check-delay-ms}")
    public void expireReservations() {
        List<Order> pendingOrders = orderRepository.findPendingPaymentWithExpiredReservation(
                PaymentStatus.PENDING,
                LocalDateTime.now()
        );

        if (pendingOrders.isEmpty()) {
            log.debug("No expired payment reservations found.");
            return;
        }

        log.info("Found {} expired reservations.", pendingOrders.size());

        for (Order order : pendingOrders) {
            try {
                // Update order status
                orderLifecycleService.transition(order, OrderStatus.RESERVATION_EXPIRED);
                orderRepository.save(order);

                paymentRepository.findByOrderId(order.getId())
                        .ifPresent(pendingPayment -> {
                            pendingPayment.setStatus(PaymentStatus.EXPIRED);
                            log.info("Updated payment status to : {} for orderId: {}", pendingPayment.getStatus(), order.getId());
                        });

                // ✅ Publish to UNIFIED QUEUE instead of direct event
                ReservationExpiredEvent event = new ReservationExpiredEvent(
                        order.getId(),
                        order.getUser().getId()
                );
                eventPublisher.publishEvent(event);

                log.info("Reservation expired and queued: orderId={}", order.getId());

            } catch (Exception e) {
                log.error("Failed to process expired reservation: orderId={}", order.getId(), e);
            }
        }
    }
}