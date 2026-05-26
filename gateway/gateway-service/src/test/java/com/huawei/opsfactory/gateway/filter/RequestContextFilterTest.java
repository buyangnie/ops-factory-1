/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.huawei.opsfactory.gateway.config.GatewayProperties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

/**
 * Test coverage for Request Context Filter.
 *
 * @author x00000000
 * @since 2026-05-26
 */
public class RequestContextFilterTest {
    private RequestContextFilter filter;

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        GatewayProperties properties = new GatewayProperties();
        filter = new RequestContextFilter(properties);
    }

    /**
     * Tests generates request id when missing.
     */
    @Test
    public void testGeneratesRequestIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gateway/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String requestId = (String) request.getAttribute(RequestContextFilter.REQUEST_ID_ATTR);
        assertNotNull(requestId);
        assertEquals(requestId, response.getHeader(RequestContextFilter.REQUEST_ID_HEADER));
    }

    /**
     * Tests reuses incoming request id.
     */
    @Test
    public void testReusesIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gateway/status");
        request.addHeader(RequestContextFilter.REQUEST_ID_HEADER, "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("req-123", request.getAttribute(RequestContextFilter.REQUEST_ID_ATTR));
        assertEquals("req-123", response.getHeader(RequestContextFilter.REQUEST_ID_HEADER));
    }
}