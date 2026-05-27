/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.model;

import java.util.List;

public record SkillDetail(
    String id,
    String name,
    String description,
    String path,
    boolean containsScripts,
    String checksum,
    long sizeBytes,
    int fileCount,
    String entrypoint,
    List<String> files,
    String instructions,
    String createdAt,
    String updatedAt
) {
}
