/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.huawei.opsfactory.gateway.common.model.ManagedInstance;
import com.huawei.opsfactory.gateway.filter.UserContextFilter;
import com.huawei.opsfactory.gateway.process.InstanceManager;
import com.huawei.opsfactory.gateway.proxy.GoosedProxy;

import reactor.core.publisher.Mono;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Test coverage for Catch All Proxy Controller.
 *
 * @author x00000000
 * @since 2026-05-26
 */
public class CatchAllProxyControllerTest {
    private InstanceManager instanceManager;

    private GoosedProxy goosedProxy;

    private CatchAllProxyController controller;

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        instanceManager = mock(InstanceManager.class);
        goosedProxy = mock(GoosedProxy.class);
        controller = new CatchAllProxyController(instanceManager, goosedProxy);
    }

    /**
     * Tests authenticated user access to status.
     */
    @Test
    public void testAuthenticatedAccess_status() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gateway/agents/test-agent/status");
        request.setAttribute(UserContextFilter.USER_ID_ATTR, "alice");

        ManagedInstance instance = new ManagedInstance("test-agent", "alice", 9000, 123L, null, "test-secret");
        when(instanceManager.getOrSpawn("test-agent", "alice")).thenReturn(Mono.just(instance));
        when(goosedProxy.fetchJson(eq(9000), any(), eq("/status"), any(), eq(30), eq("test-secret")))
            .thenReturn(Mono.just("{\"status\":\"ok\"}"));

        String result = controller.proxyStatus("test-agent", request);

        assertEquals("{\"status\":\"ok\"}", result);
        verify(instanceManager).getOrSpawn("test-agent", "alice");
    }

    /**
     * Tests user access to system info allowed.
     */
    @Test
    public void testUserAccessToSystemInfo_allowed() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gateway/agents/test-agent/system_info");
        request.setAttribute(UserContextFilter.USER_ID_ATTR, "alice");

        ManagedInstance instance = new ManagedInstance("test-agent", "alice", 9000, 123L, null, "test-secret");
        when(instanceManager.getOrSpawn("test-agent", "alice")).thenReturn(Mono.just(instance));
        when(goosedProxy.fetchJson(eq(9000), any(), eq("/system_info"), any(), eq(30), eq("test-secret")))
            .thenReturn(Mono.just("{\"info\":\"test\"}"));

        String result = controller.proxySystemInfo("test-agent", request);

        assertEquals("{\"info\":\"test\"}", result);
        verify(instanceManager).getOrSpawn("test-agent", "alice");
    }

    /**
     * Tests query string forwarded to goosed.
     */
    @Test
    public void testQueryStringForwarding() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gateway/agents/test-agent/status?limit=5");
        request.setQueryString("limit=5");
        request.setAttribute(UserContextFilter.USER_ID_ATTR, "admin");

        ManagedInstance instance = new ManagedInstance("test-agent", "admin", 9000, 123L, null, "test-secret");
        when(instanceManager.getOrSpawn("test-agent", "admin")).thenReturn(Mono.just(instance));
        when(goosedProxy.fetchJson(eq(9000), any(), eq("/status?limit=5"), any(), eq(30), eq("test-secret")))
            .thenReturn(Mono.just("{\"status\":\"ok\"}"));

        controller.proxyStatus("test-agent", request);

        verify(goosedProxy).fetchJson(eq(9000), any(), eq("/status?limit=5"), any(), eq(30), eq("test-secret"));
    }

    /**
     * Tests instance manager throws exception.
     */
    @Test
    public void testInstanceManagerThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gateway/agents/test-agent/status");
        request.setAttribute(UserContextFilter.USER_ID_ATTR, "admin");

        when(instanceManager.getOrSpawn("test-agent", "admin"))
            .thenReturn(Mono.error(new RuntimeException("Instance not found")));

        try {
            controller.proxyStatus("test-agent", request);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals("Instance not found", ex.getMessage());
        }
    }
}