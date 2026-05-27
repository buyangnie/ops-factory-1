/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.model;

import java.util.List;

public record SkillMutationResponse(
    SkillSummary skill,
    List<SkillWarning> warnings
) {
}
