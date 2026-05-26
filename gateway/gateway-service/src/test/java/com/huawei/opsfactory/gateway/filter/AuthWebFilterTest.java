/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.filter;

import static org.junit.Assert.assertEquals;

import com.huawei.opsfactory.gateway.config.GatewayProperties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

/**
 * Test coverage for Auth Web Filter.
 *
 * @author x00000000
 * @since 2026-05-26
 */
public class AuthWebFilterTest {
    private AuthWebFilter filter;

    private GatewayProperties properties;

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        properties = new GatewayProperties();
        properties.setSecretKey("test-secret");
        filter = new AuthWebFilter(properties);
    }

    /**
     * Tests status endpoint is public.
     */
    @Test
    public void testStatusEndpointIsPublic() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/status");
        request.addHeader("x-secret-key", "test-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    /**
     * Tests options passes through.
     */
    @Test
    public void testOptionsPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/agents");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    /**
     * Tests valid secret key in header.
     */
    @Test
    public void testValidSecretKeyInHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/agents");
        request.addHeader("x-secret-key", "test-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    /**
     * Tests valid secret key in query param.
     */
    @Test
    public void testValidSecretKeyInQueryParam() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/agents");
        request.addParameter("key", "test-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    /**
     * Tests invalid secret key returns401.
     */
    @Test
    public void testInvalidSecretKeyReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/agents");
        request.addHeader("x-secret-key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    /**
     * Tests missing secret key returns401.
     */
    @Test
    public void testMissingSecretKeyReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/agents");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }
}