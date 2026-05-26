/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

/**
 * Test coverage for Global Exception Handler.
 *
 * @author x00000000
 * @since 2026-05-26
 */
public class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        handler = new GlobalExceptionHandler();
    }

    /**
     * Tests handle input exception decoding error.
     */
    @Test
    public void testHandleInputException_decodingError() {
        HttpMessageNotReadableException cause = new HttpMessageNotReadableException("bad json", null, null);
        ResponseEntity<Map<String, Object>> response = handler.handleInputException(cause);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("bad json", response.getBody().get("error"));
    }

    /**
     * Tests handle input exception other error.
     */
    @Test
    public void testHandleInputException_otherError() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Missing parameter 'name'", null, null);

        ResponseEntity<Map<String, Object>> response = handler.handleInputException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("Missing parameter 'name'", response.getBody().get("error"));
    }

    /**
     * Tests handle response status exception with reason.
     */
    @Test
    public void testHandleResponseStatusException_withReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "session not found");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("session not found", response.getBody().get("error"));
    }

    /**
     * Tests handle response status exception forbidden.
     */
    @Test
    public void testHandleResponseStatusException_forbidden() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "admin access required");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("admin access required", response.getBody().get("error"));
    }

    /**
     * Tests handle response status exception no reason.
     */
    @Test
    public void testHandleResponseStatusException_noReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        // When no reason, falls back to getMessage()
        String error = (String) response.getBody().get("error");
        // getMessage() returns something like "500 INTERNAL_SERVER_ERROR"
        assertEquals(ex.getMessage(), error);
    }

    /**
     * Tests handle no resource found exception.
     */
    @Test
    public void testHandleNoResourceFoundException() {
        NoResourceFoundException ex = new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/missing/path");

        ResponseEntity<Map<String, Object>> response = handler.handleNoResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Tests handle unexpected exception.
     */
    @Test
    public void testHandleUnexpectedException() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleUnexpected(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("Internal server error", response.getBody().get("error"));
    }
}