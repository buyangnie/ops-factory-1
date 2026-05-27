/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.common.logging;

import com.huawei.opsfactory.skillmarket.config.SkillMarketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
/**
 * Skill Market Startup Logger.
 *
 * @author x00000000
 * @since 2026-05-27
 */
public class SkillMarketStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SkillMarketStartupLogger.class);

    private final SkillMarketProperties properties;

    public SkillMarketStartupLogger(SkillMarketProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(
            "skill-market startup ready baseDir={} maxUploadSizeMb={} maxUnpackedSizeMb={} maxFileCount={} exposeFileList={}",
            properties.getRuntime().getBaseDir(),
            properties.getPackage().getMaxUploadSizeMb(),
            properties.getPackage().getMaxUnpackedSizeMb(),
            properties.getPackage().getMaxFileCount(),
            properties.getPackage().isExposeFileList()
        );
    }
}
