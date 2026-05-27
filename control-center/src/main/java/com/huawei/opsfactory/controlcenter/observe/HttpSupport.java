/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.controlcenter.observe;

import com.huawei.opsfactory.controlcenter.config.ControlCenterProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
/**
 * Http Support.
 *
 * @author x00000000
 * @since 2026-05-27
 */
public class HttpSupport {

    private final ControlCenterProperties properties;
    private final HttpClient client;

    public HttpSupport(ControlCenterProperties properties) {
        this.properties = properties;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getRequestTimeoutMs()))
                .build();
    }

    public HttpResponse<String> get(String url, HttpHeaders headers) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(properties.getRequestTimeoutMs()))
                .GET();
        headers.forEach((name, values) -> values.forEach(value -> request.header(name, value)));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
