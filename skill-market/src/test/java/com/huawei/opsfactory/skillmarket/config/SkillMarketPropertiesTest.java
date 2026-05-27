/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Skill Market Properties Test.
 *
 * @author x00000000
 * @since 2026-05-27
 */
class SkillMarketPropertiesTest {

    @Test
    void defaultValuesAreConservative() {
        SkillMarketProperties properties = new SkillMarketProperties();

        assertEquals("*", properties.getCorsOrigin());
        assertEquals("./data", properties.getRuntime().getBaseDir());
        assertEquals(50, properties.getPackage().getMaxUploadSizeMb());
        assertEquals(200, properties.getPackage().getMaxUnpackedSizeMb());
        assertEquals(1000, properties.getPackage().getMaxFileCount());
        assertEquals(20, properties.getPackage().getMaxSingleFileSizeMb());
        assertTrue(properties.getPackage().isExposeFileList());
        assertTrue(properties.getPackage().isAllowScripts());
        assertTrue(properties.getLogging().isAccessLogEnabled());
    }

}

@SpringBootTest(properties = {
    "skill-market.cors-origin=http://localhost:5173",
    "skill-market.runtime.base-dir=/tmp/skill-market-test",
    "skill-market.package.max-upload-size-mb=12",
    "skill-market.package.max-unpacked-size-mb=34",
    "skill-market.package.max-file-count=56",
    "skill-market.package.max-single-file-size-mb=7",
    "skill-market.package.expose-file-list=false",
    "skill-market.package.allow-scripts=false",
    "skill-market.logging.access-log-enabled=false"
})
/**
 * Skill Market Properties Binding Test.
 *
 * @author x00000000
 * @since 2026-05-27
 */
class SkillMarketPropertiesBindingTest {

    @Autowired
    private SkillMarketProperties properties;

    @Test
    void bindsFromSpringConfiguration() {
        assertEquals("http://localhost:5173", properties.getCorsOrigin());
        assertEquals("/tmp/skill-market-test", properties.getRuntime().getBaseDir());
        assertEquals(12, properties.getPackage().getMaxUploadSizeMb());
        assertEquals(34, properties.getPackage().getMaxUnpackedSizeMb());
        assertEquals(56, properties.getPackage().getMaxFileCount());
        assertEquals(7, properties.getPackage().getMaxSingleFileSizeMb());
        assertFalse(properties.getPackage().isExposeFileList());
        assertFalse(properties.getPackage().isAllowScripts());
        assertFalse(properties.getLogging().isAccessLogEnabled());
    }
}
