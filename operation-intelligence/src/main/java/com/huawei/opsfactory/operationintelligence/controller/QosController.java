/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.operationintelligence.controller;

import com.huawei.opsfactory.operationintelligence.qos.model.ProductConfigRule;
import com.huawei.opsfactory.operationintelligence.service.QosService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Qos Controller.
 *
 * @author x00000000
 * @since 2026-05-11
 */
@RestController
@RequestMapping("/operation-intelligence/qos")
public class QosController {

    private static final Logger log = LoggerFactory.getLogger(QosController.class);

    private final QosService qosService;

    /**
     * Qos Controller.
     *
     * @param qosService the qosService
     */
    public QosController(QosService qosService) {
        this.qosService = qosService;
    }

    static long toLong(Object val) {
        if (val instanceof Number)
            return ((Number) val).longValue();
        if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid numeric value: " + val);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid numeric value: " + val);
    }

    static int toInt(Object val) {
        if (val instanceof Number)
            return ((Number) val).intValue();
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid numeric value: " + val);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid numeric value: " + val);
    }

    /**
     * Gets the health indicator.
     *
     * @param req the req
     * @return the result
     */
    @PostMapping("/getHealthIndicator")
    public Map<String, Object> getHealthIndicator(@RequestBody Map<String, Object> req) {
        String envCode = (String) req.get("envCode");
        requireEnvCode(envCode);
        long startTime = toLong(req.get("startTime"));
        long endTime = toLong(req.get("endTime"));
        validateTimeRange(startTime, endTime);
        List<Map<String, Object>> results = qosService.getHealthIndicator(envCode, startTime, endTime);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        return response;
    }

    /**
     * Gets the available indicator detail.
     *
     * @param req the req
     * @return the result
     */
    @PostMapping("/getAvailableIndicatorDetail")
    public Map<String, Object> getAvailableIndicatorDetail(@RequestBody Map<String, Object> req) {
        return getIndicatorDetail(req, "A");
    }

    /**
     * Gets the performance indicator detail.
     *
     * @param req the req
     * @return the result
     */
    @PostMapping("/getPerformanceIndicatorDetail")
    public Map<String, Object> getPerformanceIndicatorDetail(@RequestBody Map<String, Object> req) {
        return getIndicatorDetail(req, "P");
    }

    /**
     * Gets the resource indicator detail.
     *
     * @param req the req
     * @return the result
     */
    @PostMapping("/getResourceIndicatorDetail")
    public Map<String, Object> getResourceIndicatorDetail(@RequestBody Map<String, Object> req) {
        String envCode = (String) req.get("envCode");
        requireEnvCode(envCode);
        long startTime = toLong(req.get("startTime"));
        long endTime = toLong(req.get("endTime"));
        validateTimeRange(startTime, endTime);
        List<?> results = qosService.getResourceNormalize(envCode, startTime, endTime);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        return response;
    }

    /**
     * Gets the contribution data.
     *
     * @param req the req
     * @return the result
     */
    @PostMapping("/getContributionData")
    public Map<String, Object> getContributionData(@RequestBody Map<String, Object> req) {
        String envCode = (String) req.get("envCode");
        requireEnvCode(envCode);
        long startTime = toLong(req.get("startTime"));
        long endTime = toLong(req.get("endTime"));
        validateTimeRange(startTime, endTime);
        List<Map<String, Object>> results = qosService.getContributionData(envCode, startTime, endTime);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        return response;
    }

    /**
     * Gets the alarm indicator detail.
     *
     * @param req the req
     * @return the result
     */
    @PostMapping("/getAlarmIndicatorDetail")
    public Map<String, Object> getAlarmIndicatorDetail(@RequestBody Map<String, Object> req) {
        String envCode = (String) req.get("envCode");
        requireEnvCode(envCode);
        long startTime = toLong(req.get("startTime"));
        long endTime = toLong(req.get("endTime"));
        validateTimeRange(startTime, endTime);
        int pageIndex = req.containsKey("pageIndex") ? toInt(req.get("pageIndex")) : 1;
        int pageSize = req.containsKey("pageSize") ? toInt(req.get("pageSize")) : 10;
        return qosService.getAlarmDetail(envCode, startTime, endTime, pageIndex, pageSize);
    }

    /**
     * Gets the product config rule.
     *
     * @param req the req
     * @return the result
     */
    @PostMapping("/getProductConfigRule")
    public ResponseEntity<Map<String, Object>> getProductConfigRule(@RequestBody Map<String, Object> req) {
        String agentSolutionType = (String) req.get("agentSolutionType");
        Optional<ProductConfigRule> rule = qosService.getProductConfigRule(agentSolutionType);
        if (rule.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body((Map<String, Object>) null);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result", rule.get());
        return ResponseEntity.ok(body);
    }

    /**
     * Gets the environments.
     *
     * @return the result
     */
    @GetMapping("/getEnvironments")
    public Map<String, Object> getEnvironments() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", qosService.getEnvironments());
        return response;
    }

    private Map<String, Object> getIndicatorDetail(Map<String, Object> req, String type) {
        String envCode = (String) req.get("envCode");
        requireEnvCode(envCode);
        long startTime = toLong(req.get("startTime"));
        long endTime = toLong(req.get("endTime"));
        validateTimeRange(startTime, endTime);
        int pageIndex = req.containsKey("pageIndex") ? toInt(req.get("pageIndex")) : 1;
        int pageSize = req.containsKey("pageSize") ? toInt(req.get("pageSize")) : 10;
        return qosService.getIndicatorDetail(envCode, type, startTime, endTime, pageIndex, pageSize);
    }

    private void validateTimeRange(long startTime, long endTime) {
        if (startTime <= 0 || endTime <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime and endTime are required");
        }
        if (endTime <= startTime) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be greater than startTime");
        }
        long maxSpanMs = 90L * 24 * 60 * 60 * 1000;
        if (endTime - startTime > maxSpanMs) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "time range must not exceed 90 days");
        }
    }

    private void requireEnvCode(String envCode) {
        if (envCode == null || envCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "envCode is required");
        }
    }
}