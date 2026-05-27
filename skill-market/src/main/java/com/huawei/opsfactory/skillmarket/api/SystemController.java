/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.api;

import com.huawei.opsfactory.skillmarket.config.SkillMarketProperties;
import com.huawei.opsfactory.skillmarket.model.CapabilitiesResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for skill market system capabilities.
 *
 * @author x00000000
 * @since 2026-05-27
 */
@RestController("skillMarketSystemController")
@RequestMapping("/skill-market")
public class SystemController {

    private final SkillMarketProperties properties;

    public SystemController(SkillMarketProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/capabilities")
    public CapabilitiesResponse capabilities() {
        SkillMarketProperties.PackageSettings packageSettings = properties.getPackage();
        return new CapabilitiesResponse(
            List.of("zip"),
            List.of("create", "import", "list", "detail", "download", "delete"),
            new CapabilitiesResponse.PackageLimits(
                packageSettings.getMaxUploadSizeMb(),
                packageSettings.getMaxUnpackedSizeMb(),
                packageSettings.getMaxFileCount(),
                packageSettings.getMaxSingleFileSizeMb(),
                packageSettings.isAllowScripts()
            )
        );
    }
}
