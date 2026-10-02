package com.backend.ShopSphere.controller;

import com.backend.ShopSphere.dto.QueueStats;
import com.backend.ShopSphere.service.EventQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/queue")
@RequiredArgsConstructor
@Slf4j
public class QueueAdminController {

    private final EventQueueService queueService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        QueueStats stats = queueService.getQueueStats();

        Map<String, Object> status = new HashMap<>();

        status.put(
                "mainStreamSize",
                stats.getMainQueueSize()
        );

        status.put(
                "retryQueueSize",
                stats.getDelayedQueueSize()
        );

        status.put(
                "deadLetterStreamSize",
                stats.getDeadLetterQueueSize()
        );

        status.put(
                "isHealthy",
                stats.getMainQueueSize() < 1000
        );

        status.put(
                "status",
                stats.getMainQueueSize() < 100
                        ? "HEALTHY"
                        : "BUSY"
        );

        return ResponseEntity.ok(status);
    }

    @PostMapping("/replay-dlq")
    public ResponseEntity<String> replayDeadLetterQueue() {
        try {
            queueService.replayDeadLetterQueue();
            return ResponseEntity.ok("Dead letter queue replay started");
        } catch (Exception e) {
            log.error("Failed to replay DLQ: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to replay DLQ");
        }
    }
}