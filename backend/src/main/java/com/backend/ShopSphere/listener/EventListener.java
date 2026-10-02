package com.backend.ShopSphere.listener;

import com.backend.ShopSphere.events.BaseEvent;

public interface EventListener {

    void process(BaseEvent event);

    default String getConsumerName() {
        return this.getClass().getSimpleName();
    }
}