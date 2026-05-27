/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.controlcenter.model;

public record ServiceActionResult(
        String serviceId,
        String action,
        boolean success,
        long startedAt,
        long finishedAt,
        int exitCode,
        String message
) {
}
