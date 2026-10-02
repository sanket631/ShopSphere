package com.backend.ShopSphere.service;

import com.backend.ShopSphere.events.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisEventPublisher implements EventPublisher {

    private final EventQueueService queueService;

    @Override
    public void publishEvent(BaseEvent event) {
        queueService.pushEvent(event);

        log.info(
                "Event published: type={}, eventId={}, orderId={}",
                event.getEventType(),
                event.getEventId(),
                event.getOrderId()
        );
    }
}