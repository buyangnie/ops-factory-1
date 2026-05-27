/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.gateway.service;

import com.huawei.opsfactory.gateway.config.GatewayProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Proxies gateway operation-intelligence requests to the backend service.
 *
 * @author x00000000
 * @since 2026-05-20
 */
@Service
public class OperationIntelligenceProxyService {
    private static final Logger logger = LoggerFactory.getLogger(OperationIntelligenceProxyService.class);

    private static final String GATEWAY_PREFIX = "/gateway";

    private static final String OI_PATH_PREFIX = "/operation-intelligence/";

    private final GatewayProperties properties;

    private final WebClient webClient;

    /**
     * Creates an operation intelligence proxy service.
     *
     * @param properties the properties
     */
    public OperationIntelligenceProxyService(GatewayProperties properties) {
        this.properties = properties;
        int maxResponseBytes = Math.max(properties.getOperationIntelligence().getMaxResponseSizeMb(), 1) * 1024 * 1024;
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxResponseBytes))
            .build();
        this.webClient = WebClient.builder().exchangeStrategies(strategies).build();
    }

    /**
     * Proxies a request to operation-intelligence.
     *
     * @param request the request
     * @return the result
     */
    public ResponseEntity<String> proxy(HttpServletRequest request) {
        String bodyPath = targetPath(request);
        if (!bodyPath.startsWith(OI_PATH_PREFIX)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("{\"error\":\"Invalid proxy path\"}");
        }
        String baseUrl = properties.getOperationIntelligence().getBaseUrl();
        if (!baseUrl.startsWith("https://") && !properties.getOperationIntelligence().getSecretKey().isEmpty()) {
            logger.warn("Secret key transmitted over plain HTTP - use HTTPS for production");
        }
        URI targetUri = UriComponentsBuilder.fromUriString(baseUrl)
            .path(bodyPath)
            .query(request.getQueryString())
            .build(true)
            .toUri();
        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("{\"error\":\"Unsupported method\"}");
        }

        WebClient.RequestBodySpec spec = webClient.method(method)
            .uri(targetUri)
            .header("x-secret-key", properties.getOperationIntelligence().getSecretKey())
            .headers(headers -> copyForwardHeaders(request, headers));

        byte[] bodyBytes;
        try {
            bodyBytes = request.getInputStream().readAllBytes();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("{\"error\":\"Failed to read request body\"}");
        }

        String body = bodyBytes.length == 0 ? "" : new String(bodyBytes, StandardCharsets.UTF_8);
        Duration timeout = Duration.ofMillis(properties.getOperationIntelligence().getRequestTimeoutMs());
        return send(spec, body).timeout(timeout).block(timeout);
    }

    private reactor.core.publisher.Mono<ResponseEntity<String>> send(WebClient.RequestBodySpec spec, String body) {
        WebClient.RequestHeadersSpec<?> ready = body.isBlank() ? spec : spec.bodyValue(body);
        return ready.exchangeToMono(response -> response.bodyToMono(String.class)
            .defaultIfEmpty("")
            .map(responseBody -> ResponseEntity.status(response.statusCode())
                .headers(response.headers().asHttpHeaders())
                .body(responseBody)));
    }

    String targetPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith(GATEWAY_PREFIX)) {
            return path.substring(GATEWAY_PREFIX.length());
        }
        return path;
    }

    private void copyForwardHeaders(HttpServletRequest request, HttpHeaders headers) {
        String userId = request.getHeader("x-user-id");
        if (userId != null && !userId.isBlank()) {
            headers.set("x-user-id", userId);
        }
        String contentTypeValue = request.getContentType();
        MediaType contentType = contentTypeValue == null ? null : MediaType.parseMediaType(contentTypeValue);
        if (contentType != null) {
            headers.setContentType(contentType);
        }
    }
}
