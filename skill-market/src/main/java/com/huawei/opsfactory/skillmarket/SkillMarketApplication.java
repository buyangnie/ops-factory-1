/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
/**
 * Skill Market Application.
 *
 * @author x00000000
 * @since 2026-05-27
 */
public class SkillMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillMarketApplication.class, args);
    }
}
