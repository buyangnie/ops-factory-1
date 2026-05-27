/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSkillRequest(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 1000) String description,
    @NotBlank String instructions
) {
}
