/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.controlcenter.api;

import com.huawei.opsfactory.controlcenter.events.EventStoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/control-center/events")
/**
 * Event Controller.
 *
 * @author x00000000
 * @since 2026-05-27
 */
public class EventController {

    private final EventStoreService eventStoreService;

    public EventController(EventStoreService eventStoreService) {
        this.eventStoreService = eventStoreService;
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of("events", eventStoreService.list());
    }
}
