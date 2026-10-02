package com.backend.ShopSphere.service;

import com.backend.ShopSphere.config.EventStreamInitializer;
import com.backend.ShopSphere.dto.QueueStats;
import com.backend.ShopSphere.events.BaseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventQueueService {

    private final RedisTemplate<String, String> eventRedisTemplate;
    private final ObjectMapper redisObjectMapper;

    private static final String RETRY_ZSET =
            "shopsphere:events:retry";

    private static final String DLQ_STREAM =
            "shopsphere:events:dlq";

    @Value("${shopsphere.queue.max-retries:3}")
    private int maxRetries;

    @Value("${shopsphere.queue.retry-delay-ms:5000}")
    private long retryDelayMs;

    @Value("${shopsphere.queue.consumer-name:event-worker-1}")
    private String consumerName;


    // =========================================================
    // PRODUCE EVENT
    // =========================================================

    public void pushEvent(BaseEvent event) {
        try {
            String eventJson =
                    redisObjectMapper.writeValueAsString(event);

            eventRedisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(EventStreamInitializer.EVENT_STREAM)
                            .ofMap(Map.of(
                                    "eventId",
                                    event.getEventId().toString(),

                                    "eventType",
                                    event.getEventType().name(),

                                    "payload",
                                    eventJson
                            ))
            );

            log.info(
                    "Event pushed to stream: type={}, eventId={}, orderId={}",
                    event.getEventType(),
                    event.getEventId(),
                    event.getOrderId()
            );

        } catch (Exception e) {
            log.error(
                    "Failed to push event to stream: {}",
                    e.getMessage(),
                    e
            );
        }
    }


    // =========================================================
    // CONSUME EVENT
    // =========================================================

    public EventMessage popEvent() {
        try {
            List<MapRecord<String, Object, Object>> records =
                    eventRedisTemplate.opsForStream().read(
                            Consumer.from(
                                    EventStreamInitializer.EVENT_CONSUMER_GROUP,
                                    consumerName
                            ),
                            StreamReadOptions.empty()
                                    .count(1)
                                    .block(Duration.ofSeconds(5)),
                            StreamOffset.create(
                                    EventStreamInitializer.EVENT_STREAM,
                                    ReadOffset.lastConsumed()
                            )
                    );

            if (records == null || records.isEmpty()) {
                return null;
            }

            MapRecord<String, Object, Object> record =
                    records.get(0);

            Map<Object, Object> value =
                    record.getValue();

            String eventJson =
                    (String) value.get("payload");

            BaseEvent event =
                    redisObjectMapper.readValue(
                            eventJson,
                            BaseEvent.class
                    );

            return new EventMessage(
                    record.getId().getValue(),
                    event
            );

        } catch (Exception e) {
            log.error(
                    "Failed to read event from stream: {}",
                    e.getMessage(),
                    e
            );

            return null;
        }
    }


    // =========================================================
    // ACK EVENT
    // =========================================================

    public void ackEvent(String recordId) {
        try {
            eventRedisTemplate.opsForStream().acknowledge(
                    EventStreamInitializer.EVENT_STREAM,
                    EventStreamInitializer.EVENT_CONSUMER_GROUP,
                    recordId
            );

            log.debug(
                    "Event acknowledged: recordId={}",
                    recordId
            );

        } catch (Exception e) {
            log.error(
                    "Failed to acknowledge event: recordId={}, error={}",
                    recordId,
                    e.getMessage(),
                    e
            );

            throw e;
        }
    }


    // =========================================================
    // RETRY
    // =========================================================

    public boolean retryEvent(BaseEvent event) {
        try {
            event.setRetryCount(
                    event.getRetryCount() + 1
            );

            /*
             * maxRetries = 3 means:
             *
             * Initial attempt: retryCount = 0
             * Retry 1:          retryCount = 1
             * Retry 2:          retryCount = 2
             * Retry 3:          retryCount = 3
             * Next failure:     retryCount = 4 -> DLQ
             */
            if (event.getRetryCount() > maxRetries) {
                moveToDeadLetterStream(event);
                return true;
            }

            String eventJson =
                    redisObjectMapper.writeValueAsString(event);

            long nextAttemptAt =
                    System.currentTimeMillis() + retryDelayMs;

            eventRedisTemplate.opsForZSet().add(
                    RETRY_ZSET,
                    eventJson,
                    nextAttemptAt
            );

            log.warn(
                    "Event scheduled for retry: type={}, eventId={}, retryCount={}, nextAttemptAt={}",
                    event.getEventType(),
                    event.getEventId(),
                    event.getRetryCount(),
                    nextAttemptAt
            );

            return true;

        } catch (Exception e) {
            log.error(
                    "Failed to schedule event retry: eventId={}",
                    event.getEventId(),
                    e
            );

            return false;
        }
    }


    // =========================================================
    // MOVE EXHAUSTED EVENT TO DLQ STREAM
    // =========================================================

    private void moveToDeadLetterStream(BaseEvent event) throws JsonProcessingException {
        try {
            String eventJson =
                    redisObjectMapper.writeValueAsString(event);

            eventRedisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(DLQ_STREAM)
                            .ofMap(Map.of(
                                    "eventId",
                                    event.getEventId().toString(),

                                    "eventType",
                                    event.getEventType().name(),

                                    "payload",
                                    eventJson
                            ))
            );

            log.error(
                    "Event moved to DLQ stream: type={}, eventId={}, orderId={}, retryCount={}",
                    event.getEventType(),
                    event.getEventId(),
                    event.getOrderId(),
                    event.getRetryCount()
            );

        } catch (Exception e) {
            log.error(
                    "Failed to move event to DLQ stream: eventId={}",
                    event.getEventId(),
                    e
            );

            throw e;
        }
    }


    // =========================================================
    // PROCESS DUE RETRIES
    // =========================================================

    public void processRetryEvents() {

        long now =
                System.currentTimeMillis();

        try {
            Set<String> dueEvents =
                    eventRedisTemplate.opsForZSet()
                            .rangeByScore(
                                    RETRY_ZSET,
                                    0,
                                    now
                            );

            if (dueEvents == null || dueEvents.isEmpty()) {
                return;
            }

            for (String eventJson : dueEvents) {

                BaseEvent event =
                        redisObjectMapper.readValue(
                                eventJson,
                                BaseEvent.class
                        );

                /*
                 * Put the retry back into the main
                 * Redis Stream.
                 */
                eventRedisTemplate.opsForStream().add(
                        StreamRecords.newRecord()
                                .in(EventStreamInitializer.EVENT_STREAM)
                                .ofMap(Map.of(
                                        "eventId",
                                        event.getEventId().toString(),

                                        "eventType",
                                        event.getEventType().name(),

                                        "payload",
                                        eventJson
                                ))
                );

                /*
                 * Remove it from the retry scheduler
                 * only after successfully adding it
                 * to the Stream.
                 */
                eventRedisTemplate.opsForZSet().remove(
                        RETRY_ZSET,
                        eventJson
                );

                log.info(
                        "Retry event moved back to stream: eventId={}, retryCount={}",
                        event.getEventId(),
                        event.getRetryCount()
                );
            }

        } catch (Exception e) {
            log.error(
                    "Failed to process retry events",
                    e
            );
        }
    }


    // =========================================================
    // QUEUE STATISTICS
    // =========================================================

    public QueueStats getQueueStats() {
        try {

            Long mainStreamSize =
                    eventRedisTemplate.opsForStream()
                            .size(
                                    EventStreamInitializer.EVENT_STREAM
                            );

            Long retryQueueSize =
                    eventRedisTemplate.opsForZSet()
                            .size(RETRY_ZSET);

            Long dlqStreamSize =
                    eventRedisTemplate.opsForStream()
                            .size(DLQ_STREAM);

            return QueueStats.builder()
                    .mainQueueSize(
                            mainStreamSize != null
                                    ? mainStreamSize
                                    : 0L
                    )
                    .delayedQueueSize(
                            retryQueueSize != null
                                    ? retryQueueSize
                                    : 0L
                    )
                    .deadLetterQueueSize(
                            dlqStreamSize != null
                                    ? dlqStreamSize
                                    : 0L
                    )
                    .build();

        } catch (Exception e) {

            log.error(
                    "Failed to get queue stats: {}",
                    e.getMessage(),
                    e
            );

            return QueueStats.builder()
                    .mainQueueSize(0L)
                    .delayedQueueSize(0L)
                    .deadLetterQueueSize(0L)
                    .build();
        }
    }


    // =========================================================
    // REPLAY DLQ
    // =========================================================

    public void replayDeadLetterQueue() throws JsonProcessingException {

        try {

            List<MapRecord<String, Object, Object>> records =
                    eventRedisTemplate.opsForStream().read(
                            StreamReadOptions.empty()
                                    .count(1),
                            StreamOffset.fromStart(DLQ_STREAM)
                    );

            if (records == null || records.isEmpty()) {
                log.info("DLQ is empty");
                return;
            }

            MapRecord<String, Object, Object> record =
                    records.get(0);

            Map<Object, Object> value =
                    record.getValue();

            String eventJson =
                    (String) value.get("payload");

            BaseEvent event =
                    redisObjectMapper.readValue(
                            eventJson,
                            BaseEvent.class
                    );

            /*
             * Reset retry count when manually replaying
             * an event from the DLQ.
             */
            event.setRetryCount(0);

            String newEventJson =
                    redisObjectMapper.writeValueAsString(event);

            /*
             * Put the event back into the MAIN stream.
             */
            eventRedisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(EventStreamInitializer.EVENT_STREAM)
                            .ofMap(Map.of(
                                    "eventId",
                                    event.getEventId().toString(),

                                    "eventType",
                                    event.getEventType().name(),

                                    "payload",
                                    newEventJson
                            ))
            );

            /*
             * Remove the event from the DLQ only after
             * successfully adding it to the main stream.
             */
            eventRedisTemplate.opsForStream().delete(
                    DLQ_STREAM,
                    record.getId()
            );

            log.info(
                    "Replayed event from DLQ: eventId={}, dlqRecordId={}",
                    event.getEventId(),
                    record.getId()
            );

        } catch (Exception e) {

            log.error(
                    "Failed to replay DLQ",
                    e
            );

            throw e;
        }
    }


    // =========================================================
    // EVENT MESSAGE
    // =========================================================

    public record EventMessage(
            String recordId,
            BaseEvent event
    ) {
    }
}