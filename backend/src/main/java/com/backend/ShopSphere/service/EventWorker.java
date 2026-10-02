package com.backend.ShopSphere.service;

import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.enums.EventType;
import com.backend.ShopSphere.listener.EventListener;
import com.backend.ShopSphere.listener.EventListenerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventWorker {

    private final EventQueueService queueService;
    private final EventListenerRegistry listenerRegistry;
    private final EventConsumerProcessor eventConsumerProcessor;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(true);

    @PostConstruct
    public void startWorker() {
        log.info("Starting EventWorker...");
        executor.submit(this::processEvents);
    }

    @PreDestroy
    public void stopWorker() {
        log.info("Stopping EventWorker...");
        running.set(false);
        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void processEvents() {
        log.info("EventWorker started");

        while (running.get()) {
            try {
                queueService.processRetryEvents();

                EventQueueService.EventMessage message =
                        queueService.popEvent();

                if (message == null) {
                    Thread.sleep(1000);
                    continue;
                }

                processEvent(message);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {
                log.error(
                        "Error processing event: {}",
                        e.getMessage(),
                        e
                );

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processEvent(EventQueueService.EventMessage message) {

        BaseEvent event = message.event();

        UUID eventId = event.getEventId();
        EventType eventType = event.getEventType();

        log.info(
                "Processing event: type={}, eventId={}, orderId={}, retryCount={}",
                eventType,
                eventId,
                event.getOrderId(),
                event.getRetryCount()
        );

        try {
            List<EventListener> listeners =
                    listenerRegistry.getListeners(eventType);

            if (listeners.isEmpty()) {
                log.warn(
                        "No listeners registered for event type: {}",
                        eventType
                );

                queueService.ackEvent(message.recordId());
                return;
            }

            boolean anyListenerFailed = false;

            for (EventListener listener : listeners) {
                try {
                    eventConsumerProcessor.process(event, listener);

                } catch (Exception e) {
                    log.error(
                            "Listener {} failed for event {}",
                            listener.getConsumerName(),
                            event.getEventId(),
                            e
                    );

                    anyListenerFailed = true;
                }
            }

            if (anyListenerFailed) {
                log.warn(
                        "Some listeners failed for event: type={}, eventId={}",
                        eventType,
                        eventId
                );

                throw new RuntimeException(
                        "Some listeners failed for event: " + eventId
                );
            }

            queueService.ackEvent(message.recordId());

            log.info(
                    "Event processed and acknowledged successfully: " +
                            "type={}, eventId={}, orderId={}, recordId={}",
                    eventType,
                    eventId,
                    event.getOrderId(),
                    message.recordId()
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process event: type={}, eventId={}, error={}",
                    eventType,
                    eventId,
                    e.getMessage(),
                    e
            );

            boolean retryScheduled = queueService.retryEvent(event);

            if (retryScheduled) {
                queueService.ackEvent(message.recordId());
            } else {
                log.error(
                        "Retry could not be scheduled. " +
                                "Leaving event pending for recovery: eventId={}, recordId={}",
                        eventId,
                        message.recordId()
                );
            }
        }
    }
}