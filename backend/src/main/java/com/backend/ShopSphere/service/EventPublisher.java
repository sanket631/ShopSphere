package com.backend.ShopSphere.service;

import com.backend.ShopSphere.events.BaseEvent;

public interface EventPublisher {
    void publishEvent(BaseEvent event);
}
