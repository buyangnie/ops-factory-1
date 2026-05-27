/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.controlcenter.model;

public record ManagedServiceStatus(
        String id,
        String name,
        boolean required,
        String status,
        boolean reachable,
        String host,
        String healthPath,
        long checkedAt,
        String message
) {
}
