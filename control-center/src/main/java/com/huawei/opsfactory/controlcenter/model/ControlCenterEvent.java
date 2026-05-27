/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.controlcenter.model;

public record ControlCenterEvent(
        long timestamp,
        String type,
        String serviceId,
        String serviceName,
        String level,
        String message
) {
}
