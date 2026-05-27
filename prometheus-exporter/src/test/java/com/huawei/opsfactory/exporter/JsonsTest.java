/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.exporter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Jsons Test.
 *
 * @author x00000000
 * @since 2026-05-27
 */
class JsonsTest {

    // --- asMap ---

    @Test
    void asMapParsesValidJson() throws IOException {
        Map<String, Object> result = Jsons.asMap("{\"a\":1,\"b\":\"hello\"}");
        Assertions.assertEquals(1, result.get("a"));
        Assertions.assertEquals("hello", result.get("b"));
    }

    @Test
    void asMapThrowsOnMalformedJson() {
        Assertions.assertThrows(IOException.class, () -> Jsons.asMap("{invalid"));
    }

    @Test
    void asMapParsesEmptyObject() throws IOException {
        Map<String, Object> result = Jsons.asMap("{}");
        Assertions.assertTrue(result.isEmpty());
    }

    // --- asNumber ---

    @Test
    void asNumberReturnsNumberDirectly() {
        Assertions.assertEquals(42, Jsons.asNumber(42).intValue());
        Assertions.assertEquals(3.14, Jsons.asNumber(3.14).doubleValue(), 0.001);
    }

    @Test
    void asNumberReturnsZeroForNull() {
        Assertions.assertEquals(0, Jsons.asNumber(null).intValue());
    }

    @Test
    void asNumberParsesStringNumber() {
        Assertions.assertEquals(99.5, Jsons.asNumber("99.5").doubleValue(), 0.001);
    }

    @Test
    void asNumberReturnsZeroForNonNumericString() {
        Assertions.assertEquals(0, Jsons.asNumber("abc").intValue());
    }

    // --- asMapSafe ---

    @Test
    void asMapSafeReturnsMapWhenGivenMap() {
        Map<String, Object> input = Map.of("key", "value");
        Map<String, Object> result = Jsons.asMapSafe(input);
        Assertions.assertEquals("value", result.get("key"));
    }

    @Test
    void asMapSafeReturnsEmptyForNonMap() {
        Assertions.assertTrue(Jsons.asMapSafe("not a map").isEmpty());
        Assertions.assertTrue(Jsons.asMapSafe(123).isEmpty());
        Assertions.assertTrue(Jsons.asMapSafe(null).isEmpty());
    }

    // --- asListOfMaps ---

    @Test
    void asListOfMapsReturnsListWhenGivenList() {
        List<Map<String, Object>> input = List.of(Map.of("a", 1));
        List<Map<String, Object>> result = Jsons.asListOfMaps(input);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1, result.get(0).get("a"));
    }

    @Test
    void asListOfMapsReturnsEmptyForNonList() {
        Assertions.assertTrue(Jsons.asListOfMaps("not a list").isEmpty());
        Assertions.assertTrue(Jsons.asListOfMaps(null).isEmpty());
        Assertions.assertTrue(Jsons.asListOfMaps(42).isEmpty());
    }

    // --- asBoolean ---

    @Test
    void asBooleanReturnsBooleanDirectly() {
        Assertions.assertTrue(Jsons.asBoolean(true));
        Assertions.assertFalse(Jsons.asBoolean(false));
    }

    @Test
    void asBooleanReturnsFalseForNull() {
        Assertions.assertFalse(Jsons.asBoolean(null));
    }

    @Test
    void asBooleanParsesStringTrue() {
        Assertions.assertTrue(Jsons.asBoolean("true"));
        Assertions.assertFalse(Jsons.asBoolean("false"));
        Assertions.assertFalse(Jsons.asBoolean("anything"));
    }
}
