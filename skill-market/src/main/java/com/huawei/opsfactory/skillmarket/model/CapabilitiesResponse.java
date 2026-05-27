/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.model;

import java.util.List;

public record CapabilitiesResponse(
    List<String> packageFormats,
    List<String> actions,
    PackageLimits limits
) {

    public record PackageLimits(
        int maxUploadSizeMb,
        int maxUnpackedSizeMb,
        int maxFileCount,
        int maxSingleFileSizeMb,
        boolean allowScripts
    ) {
    }
}
