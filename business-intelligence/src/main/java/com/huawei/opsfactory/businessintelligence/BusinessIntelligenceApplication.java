/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.businessintelligence;

import com.huawei.opsfactory.businessintelligence.config.BusinessIntelligenceRuntimeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BusinessIntelligenceRuntimeProperties.class)
/**
 * Business Intelligence Application.
 *
 * @author x00000000
 * @since 2026-05-27
 */
public class BusinessIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessIntelligenceApplication.class, args);
    }
}

