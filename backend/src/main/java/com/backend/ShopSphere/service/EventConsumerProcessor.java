package com.backend.ShopSphere.service;

import com.backend.ShopSphere.entity.ProcessedEvent;
import com.backend.ShopSphere.events.BaseEvent;
import com.backend.ShopSphere.listener.EventListener;
import com.backend.ShopSphere.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventConsumerProcessor {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void process(
            BaseEvent event,
            EventListener listener
    ) {

        UUID eventId = event.getEventId();
        String consumer = listener.getConsumerName();

        if (processedEventRepository.existsByEventIdAndConsumer(eventId, consumer)) {
            return;
        }

        listener.process(event);

        ProcessedEvent processed = ProcessedEvent.builder()
                .eventId(eventId)
                .consumer(consumer)
                .processedAt(LocalDateTime.now())
                .build();

        processedEventRepository.save(processed);
    }
}
