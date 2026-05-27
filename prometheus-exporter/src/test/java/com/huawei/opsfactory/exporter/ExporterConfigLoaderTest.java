/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exporter Config Loader Test.
 *
 * @author x00000000
 * @since 2026-05-27
 */
class ExporterConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsAllFieldsFromYaml() throws IOException {
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
            port: 9999
            gatewayUrl: http://127.0.0.1:3000
            gatewaySecretKey: my-secret
            collectTimeoutMs: 8000
            """);

        ExporterProperties props = loadWithConfig(configFile);

        Assertions.assertEquals(9999, props.getPort());
        Assertions.assertEquals("http://127.0.0.1:3000", props.getGatewayUrl());
        Assertions.assertEquals("my-secret", props.getGatewaySecretKey());
        Assertions.assertEquals(8000, props.getCollectTimeoutMs());
    }

    @Test
    void trimsTrailingSlashFromGatewayUrl() throws IOException {
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
            gatewayUrl: http://localhost:3000///
            gatewaySecretKey: key
            """);

        ExporterProperties props = loadWithConfig(configFile);
        Assertions.assertEquals("http://localhost:3000", props.getGatewayUrl());
    }

    @Test
    void usesDefaultsWhenConfigFileDoesNotExist() {
        Path missing = tempDir.resolve("nonexistent.yaml");
        // Without gatewayUrl/gatewaySecretKey this should fail validation
        Assertions.assertThrows(IllegalStateException.class, () -> loadWithConfig(missing));
    }

    @Test
    void throwsWhenGatewayUrlMissing() throws IOException {
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
            gatewaySecretKey: key
            """);

        IllegalStateException ex = Assertions.assertThrows(
            IllegalStateException.class, () -> loadWithConfig(configFile));
        Assertions.assertTrue(ex.getMessage().contains("gatewayUrl"));
    }

    @Test
    void throwsWhenGatewaySecretKeyMissing() throws IOException {
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
            gatewayUrl: http://localhost:3000
            """);

        IllegalStateException ex = Assertions.assertThrows(
            IllegalStateException.class, () -> loadWithConfig(configFile));
        Assertions.assertTrue(ex.getMessage().contains("gatewaySecretKey"));
    }

    @Test
    void throwsOnInvalidPortValue() throws IOException {
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
            port: abc
            gatewayUrl: http://localhost:3000
            gatewaySecretKey: key
            """);

        Assertions.assertThrows(IllegalStateException.class, () -> loadWithConfig(configFile));
    }

    @Test
    void partialYamlUsesDefaults() throws IOException {
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
            gatewayUrl: http://localhost:3000
            gatewaySecretKey: key
            """);

        ExporterProperties props = loadWithConfig(configFile);
        Assertions.assertEquals(9091, props.getPort());
        Assertions.assertEquals(5000, props.getCollectTimeoutMs());
    }

    @Test
    void emptyYamlFileThrowsValidation() throws IOException {
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, "");

        Assertions.assertThrows(IllegalStateException.class, () -> loadWithConfig(configFile));
    }

    /**
     * Helper that creates an ExporterConfigLoader, sets CONFIG_PATH via a
     * package-private-friendly approach, and invokes the bean method.
     *
     * Since ExporterConfigLoader reads CONFIG_PATH from System.getenv() which
     * cannot be easily overridden, we use a subclass to inject the path.
     *
     * @author x00000000
     * @since 2026-05-27
    */
    private ExporterProperties loadWithConfig(Path configPath) {
        ExporterConfigLoader loader = new ExporterConfigLoader() {
            @Override
            protected Path getConfigPath() {
                return configPath;
            }
        };
        return loader.exporterProperties();
    }
}
