/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.controller;

import com.huawei.opsfactory.gateway.config.GatewayProperties;
import com.huawei.opsfactory.gateway.filter.UserContextFilter;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight controller exposing health-check, current-user identity, and public config.
 *
 * @author x00000000
 * @since 2026-05-09
 */
@RestController("gatewayStatusController")
@RequestMapping(value = "/gateway")
public class StatusController {
    private final GatewayProperties properties;

    /**
     * Creates the status controller.
     *
     * @param properties gateway configuration properties
     */
    public StatusController(GatewayProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns health check status.
     *
     * @return the string "ok"
     */
    @GetMapping("/status")
    public String status() {
        return "ok";
    }

    /**
     * Returns the current user's identity.
     *
     * @param request current HTTP request carrying user context attributes
     * @return a map containing "userId" and "role"
     */
    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        String userId = (String) request.getAttribute(UserContextFilter.USER_ID_ATTR);
        return Map.of("userId", userId != null ? userId : "unknown", "role", "user");
    }

    /**
     * Returns public configuration such as Office preview settings.
     *
     * @return a map with Office preview configuration
     */
    @GetMapping("/config")
    public Map<String, Object> config() {
        GatewayProperties.OfficePreview op = properties.getOfficePreview();
        return Map.of("officePreview", Map.of("enabled", op.isEnabled(), "onlyofficeUrl", op.getOnlyofficeUrl(),
            "fileBaseUrl", op.getFileBaseUrl()));
    }
}
