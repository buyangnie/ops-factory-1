/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.huawei.opsfactory.gateway.config.GatewayProperties;

import org.junit.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

/**
 * Test coverage for operation intelligence proxy service.
 *
 * @author x00000000
 * @since 2026-05-20
 */
public class OperationIntelligenceProxyServiceTest {
    /**
     * Tests gateway path prefix is removed before forwarding.
     */
    @Test
    public void testTargetPathStripsGatewayPrefix() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/gateway/operation-intelligence/graph/resources/tree?envCode=prod")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        OperationIntelligenceProxyService service = new OperationIntelligenceProxyService(new GatewayProperties());

        assertEquals("/operation-intelligence/graph/resources/tree", service.targetPath(exchange));
    }

    /**
     * Tests operation intelligence service path is preserved when called without gateway prefix.
     */
    @Test
    public void testTargetPathPreservesBackendPath() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/operation-intelligence/graph/resources/tree?envCode=prod")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        OperationIntelligenceProxyService service = new OperationIntelligenceProxyService(new GatewayProperties());

        assertEquals("/operation-intelligence/graph/resources/tree", service.targetPath(exchange));
    }

    /**
     * Tests operation intelligence proxy has a response buffer above Spring WebClient default.
     */
    @Test
    public void testOperationIntelligenceProxyResponseLimitSupportsLargeGraphExports() {
        GatewayProperties properties = new GatewayProperties();

        assertTrue(properties.getOperationIntelligence().getMaxResponseSizeMb() >= 10);
        new OperationIntelligenceProxyService(properties);
    }
}
