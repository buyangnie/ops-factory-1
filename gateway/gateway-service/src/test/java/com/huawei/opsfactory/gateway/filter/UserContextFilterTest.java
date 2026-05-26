/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.huawei.opsfactory.gateway.process.PrewarmService;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

/**
 * Test coverage for User Context Filter.
 *
 * @author x00000000
 * @since 2026-05-26
 */
public class UserContextFilterTest {
    private UserContextFilter filter;

    private PrewarmService prewarmService;

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        prewarmService = mock(PrewarmService.class);
        filter = new UserContextFilter(prewarmService);
    }

    /**
     * Tests extracts user id from header.
     */
    @Test
    public void testExtractsUserIdFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader("x-user-id", "user123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("user123", request.getAttribute(UserContextFilter.USER_ID_ATTR));
    }

    /**
     * Tests rejects400 when no user id header.
     */
    @Test
    public void testRejects400WhenNoUserIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertNull(request.getAttribute(UserContextFilter.USER_ID_ATTR));
    }

    /**
     * Tests system endpoints pass through without user context.
     */
    @Test
    public void testSystemEndpointPassesThroughWithoutUserContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gateway/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(request.getAttribute(UserContextFilter.USER_ID_ATTR));
    }

    /**
     * Tests empty user id returns400.
     */
    @Test
    public void testEmptyUserIdReturns400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader("x-user-id", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertNull(request.getAttribute(UserContextFilter.USER_ID_ATTR));
    }

    /**
     * Tests trace start does not prewarm user.
     */
    @Test
    public void testTraceStartDoesNotPrewarmUser() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest("POST", "/gateway/agents/qa-agent/sessions/20260429_3/trace");
        request.addHeader("x-user-id", "admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("admin", request.getAttribute(UserContextFilter.USER_ID_ATTR));
        verify(prewarmService, never()).onUserActivity("admin");
    }

    /**
     * Tests trace download does not prewarm user.
     */
    @Test
    public void testTraceDownloadDoesNotPrewarmUser() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/gateway/session-traces/job-1/download");
        request.addHeader("x-user-id", "admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("admin", request.getAttribute(UserContextFilter.USER_ID_ATTR));
        verify(prewarmService, never()).onUserActivity("admin");
    }

    /**
     * Tests regular gateway request prewarms user.
     */
    @Test
    public void testRegularGatewayRequestPrewarmsUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gateway/agents");
        request.addHeader("x-user-id", "admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(prewarmService).onUserActivity("admin");
    }
}