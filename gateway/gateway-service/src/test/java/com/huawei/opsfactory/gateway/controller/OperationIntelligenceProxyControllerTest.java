/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.controller;

import com.huawei.opsfactory.gateway.config.GatewayProperties;
import com.huawei.opsfactory.gateway.filter.AuthWebFilter;
import com.huawei.opsfactory.gateway.filter.UserContextFilter;
import com.huawei.opsfactory.gateway.process.PrewarmService;
import com.huawei.opsfactory.gateway.service.OperationIntelligenceProxyService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Test coverage for operation intelligence proxy controller.
 *
 * @author x00000000
 * @since 2026-05-20
 */
@RunWith(SpringRunner.class)
@WebFluxTest(OperationIntelligenceProxyController.class)
@Import({GatewayProperties.class, AuthWebFilter.class, UserContextFilter.class})
public class OperationIntelligenceProxyControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OperationIntelligenceProxyService proxyService;

    @MockBean
    private PrewarmService prewarmService;

    /**
     * Tests graph proxy.
     */
    @Test
    public void testGraphProxy() {
        Mockito.when(proxyService.proxy(ArgumentMatchers.any(ServerWebExchange.class)))
            .thenReturn(Mono.just(ResponseEntity.ok("{\"success\":true}")));

        webTestClient.post()
            .uri("/gateway/operation-intelligence/graph/resources/tree?envCode=prod")
            .header("x-secret-key", "test")
            .header("x-user-id", "kg-test-user")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.success")
            .isEqualTo(true);

        Mockito.verify(proxyService).proxy(ArgumentMatchers.any(ServerWebExchange.class));
    }

    /**
     * Tests unauthorized proxy.
     */
    @Test
    public void testUnauthorizedProxy() {
        webTestClient.get()
            .uri("/gateway/operation-intelligence/graph/resources/tree?envCode=prod")
            .exchange()
            .expectStatus()
            .isUnauthorized();

        Mockito.verifyNoInteractions(proxyService);
    }
}
