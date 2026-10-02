package com.backend.ShopSphere.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventStreamInitializer {

    public static final String EVENT_STREAM = "shopsphere:events";

    public static final String EVENT_CONSUMER_GROUP =
            "shopsphere-event-workers";

    private final RedisTemplate<String, String> eventRedisTemplate;

    @PostConstruct
    public void initialize() {
        try {
            eventRedisTemplate.opsForStream().createGroup(
                    EVENT_STREAM,
                    ReadOffset.from("0-0"),
                    EVENT_CONSUMER_GROUP
            );

            log.info(
                    "Created Redis consumer group '{}' for stream '{}'",
                    EVENT_CONSUMER_GROUP,
                    EVENT_STREAM
            );

        } catch (Exception e) {

            Throwable cause = e;

            boolean groupAlreadyExists = false;

            while (cause != null) {
                if (cause.getMessage() != null
                        && cause.getMessage().contains("BUSYGROUP")) {
                    groupAlreadyExists = true;
                    break;
                }

                cause = cause.getCause();
            }

            if (groupAlreadyExists) {
                log.info(
                        "Redis consumer group '{}' already exists",
                        EVENT_CONSUMER_GROUP
                );
            } else {
                log.error("Failed to initialize Redis event stream", e);
                throw e;
            }
        }
    }
}