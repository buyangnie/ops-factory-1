/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.controller;

import com.huawei.opsfactory.gateway.service.OperationIntelligenceProxyService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Gateway proxy for operation-intelligence endpoints.
 *
 * @author x00000000
 * @since 2026-05-20
 */
@RestController
public class OperationIntelligenceProxyController {
    private final OperationIntelligenceProxyService proxyService;

    public OperationIntelligenceProxyController(OperationIntelligenceProxyService proxyService) {
        this.proxyService = proxyService;
    }

    /**
     * Proxies all operation-intelligence requests through the gateway.
     *
     * @param exchange the exchange
     * @return the result
     */
    @RequestMapping("/gateway/operation-intelligence/**")
    public Mono<ResponseEntity<String>> proxy(ServerWebExchange exchange) {
        return proxyService.proxy(exchange);
    }
}
