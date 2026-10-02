package com.backend.ShopSphere.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStats {
    private Long mainQueueSize;
    private Long delayedQueueSize;
    private Long deadLetterQueueSize;
}