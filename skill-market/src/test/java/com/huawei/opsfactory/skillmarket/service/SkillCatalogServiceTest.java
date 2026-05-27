/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.opsfactory.skillmarket.common.error.ApiConflictException;
import com.huawei.opsfactory.skillmarket.config.SkillMarketProperties;
import com.huawei.opsfactory.skillmarket.model.CreateSkillRequest;
import com.huawei.opsfactory.skillmarket.model.SkillDetail;
import com.huawei.opsfactory.skillmarket.model.SkillMutationResponse;
import com.huawei.opsfactory.skillmarket.model.UpdateSkillRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Skill Catalog Service Test.
 *
 * @author x00000000
 * @since 2026-05-27
 */
class SkillCatalogServiceTest {

    @TempDir
    Path tempDir;

    private SkillCatalogService service;
    private SkillMarketProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SkillMarketProperties();
        properties.getRuntime().setBaseDir(tempDir.resolve("data").toString());
        service = new SkillCatalogService(properties);
    }

    @Test
    void createSkillBuildsPackageAndMetadata() throws IOException {
        SkillMutationResponse response = service.createSkill(new CreateSkillRequest(
            "log-analysis",
            "Log Analysis",
            "Analyze logs",
            "# Log Analysis\n\nFollow the runbook."
        ));

        assertEquals("log-analysis", response.skill().id());
        assertEquals("Log Analysis", response.skill().name());
        assertFalse(response.skill().containsScripts());
        assertTrue(response.skill().checksum().startsWith("sha256:"));
        assertTrue(Files.isRegularFile(tempDir.resolve("data/skills/log-analysis/package.zip")));
        assertEquals(1, service.listSkills("").size());
    }

    @Test
    void createSkillAcceptsCompleteSkillMarkdown() throws IOException {
        String markdown = """
            ---
            name: full-markdown
            description: Full markdown skill
            ---

            # Full Markdown

            ## Workflow

            1. Follow the standard skill format.
            """;

        service.createSkill(new CreateSkillRequest(
            "full-markdown",
            "Full Markdown",
            "Full markdown skill",
            markdown
        ));

        String skillMd = Files.readString(tempDir.resolve("data/skills/full-markdown/unpacked/SKILL.md"));
        assertTrue(skillMd.startsWith("---\nname: \"Full Markdown\""));
        assertTrue(skillMd.contains("description: \"Full markdown skill\""));
        assertEquals(2, countOccurrences(skillMd, "---"));
    }

    @Test
    void importSkillAcceptsRootDirectoryPackage() throws IOException {
        MockMultipartFile file = zipUpload(
            "log-analysis.zip",
            entry("log-analysis/SKILL.md", "---\nname: Log Analysis\ndescription: Analyze logs\n---\n# Log Analysis\n"),
            entry("log-analysis/scripts/analyze.py", "print('ok')\n")
        );

        SkillMutationResponse response = service.importSkill(file, null);
        SkillDetail detail = service.getSkill("log-analysis");

        assertEquals("log-analysis", response.skill().id());
        assertEquals("Log Analysis", response.skill().name());
        assertTrue(response.skill().containsScripts());
        assertEquals(1, response.warnings().size());
        assertTrue(detail.files().contains("SKILL.md"));
        assertTrue(detail.files().contains("scripts/analyze.py"));
    }

    @Test
    void importSkillAcceptsFlatPackageWithRequestedId() throws IOException {
        MockMultipartFile file = zipUpload(
            "skill.zip",
            entry("SKILL.md", "---\nname: Flat Skill\ndescription: Flat package\n---\n# Flat\n"),
            entry("references/readme.md", "# Reference\n")
        );

        SkillMutationResponse response = service.importSkill(file, "flat-skill");
        SkillDetail detail = service.getSkill("flat-skill");

        assertEquals("flat-skill", response.skill().id());
        assertEquals("Flat Skill", response.skill().name());
        assertFalse(response.skill().containsScripts());
        assertTrue(detail.files().contains("references/readme.md"));
    }

    @Test
    void listSkillsFiltersByQuery() throws IOException {
        service.createSkill(new CreateSkillRequest("log-analysis", "Log Analysis", "Analyze logs", "# Log\n"));
        service.createSkill(new CreateSkillRequest("report-builder", "Report Builder", "Build reports", "# Report\n"));

        assertEquals(2, service.listSkills("").size());
        assertEquals(1, service.listSkills("report").size());
        assertEquals("report-builder", service.listSkills("report").get(0).id());
    }

    @Test
    void deleteSkillRemovesPackage() throws IOException {
        service.createSkill(new CreateSkillRequest("log-analysis", "Log Analysis", "Analyze logs", "# Log\n"));

        service.deleteSkill("log-analysis");

        assertTrue(service.listSkills("").isEmpty());
        assertFalse(Files.exists(tempDir.resolve("data/skills/log-analysis")));
    }

    @Test
    void updateSkillReplacesSkillMarkdownAndMetadata() throws IOException {
        service.createSkill(new CreateSkillRequest("log-analysis", "Log Analysis", "Analyze logs", "# Log\n"));

        SkillMutationResponse response = service.updateSkill("log-analysis", new UpdateSkillRequest(
            "Log Analysis Updated",
            "Updated description",
            "---\nname: stale-name\ndescription: stale description\n---\n# Updated\n"
        ));
        SkillDetail detail = service.getSkill("log-analysis");

        assertEquals("log-analysis", response.skill().id());
        assertEquals("Log Analysis Updated", response.skill().name());
        assertEquals("Updated description", detail.description());
        assertTrue(detail.instructions().contains("# Updated"));
        String skillMd = Files.readString(tempDir.resolve("data/skills/log-analysis/unpacked/SKILL.md"));
        assertTrue(skillMd.contains("name: \"Log Analysis Updated\""));
        assertTrue(skillMd.contains("description: \"Updated description\""));
        assertFalse(skillMd.contains("stale-name"));
        assertTrue(skillMd.contains("# Updated"));
    }

    @Test
    void updateSkillPreservesImportedSupportFiles() throws IOException {
        MockMultipartFile file = zipUpload(
            "log-analysis.zip",
            entry("log-analysis/SKILL.md", "---\nname: Log Analysis\ndescription: Analyze logs\n---\n# Log Analysis\n"),
            entry("log-analysis/scripts/analyze.py", "print('ok')\n"),
            entry("log-analysis/references/runbook.md", "# Runbook\n")
        );
        service.importSkill(file, null);

        SkillMutationResponse response = service.updateSkill("log-analysis", new UpdateSkillRequest(
            "Log Analysis Updated",
            "Updated description",
            "---\nname: log-analysis\n---\n# Updated\n"
        ));
        SkillDetail detail = service.getSkill("log-analysis");

        assertTrue(response.skill().containsScripts());
        assertEquals(3, response.skill().fileCount());
        assertTrue(detail.files().contains("scripts/analyze.py"));
        assertTrue(detail.files().contains("references/runbook.md"));
        assertTrue(Files.isRegularFile(tempDir.resolve("data/skills/log-analysis/unpacked/scripts/analyze.py")));
        assertTrue(Files.isRegularFile(tempDir.resolve("data/skills/log-analysis/unpacked/references/runbook.md")));
    }

    @Test
    void updateSkillRequiresExistingSkill() {
        assertThrows(IllegalArgumentException.class, () -> service.updateSkill("missing-skill", new UpdateSkillRequest(
            "Missing",
            "",
            "# Missing\n"
        )));
    }

    @Test
    void duplicateCreateReturnsConflict() throws IOException {
        service.createSkill(new CreateSkillRequest("log-analysis", "Log Analysis", "Analyze logs", "# Log\n"));

        assertThrows(ApiConflictException.class, () -> service.createSkill(new CreateSkillRequest(
            "log-analysis",
            "Log Analysis 2",
            "Analyze logs",
            "# Log\n"
        )));
    }

    @Test
    void importSkillRejectsMissingSkillMd() throws IOException {
        MockMultipartFile file = zipUpload("missing.zip", entry("readme.md", "# Missing\n"));

        assertThrows(IllegalArgumentException.class, () -> service.importSkill(file, "missing-skill"));
    }

    @Test
    void importSkillRejectsInvalidRequestedId() throws IOException {
        MockMultipartFile file = zipUpload("skill.zip", entry("SKILL.md", "# Skill\n"));

        assertThrows(IllegalArgumentException.class, () -> service.importSkill(file, "Bad_Id"));
    }

    @Test
    void importSkillRejectsUnsafePath() throws IOException {
        MockMultipartFile file = zipUpload(
            "unsafe.zip",
            entry("../SKILL.md", "# Unsafe\n")
        );

        assertThrows(IllegalArgumentException.class, () -> service.importSkill(file, "unsafe"));
    }

    @Test
    void importSkillRejectsAbsolutePath() throws IOException {
        MockMultipartFile file = zipUpload(
            "unsafe.zip",
            entry("/tmp/SKILL.md", "# Unsafe\n")
        );

        assertThrows(IllegalArgumentException.class, () -> service.importSkill(file, "unsafe"));
    }

    @Test
    void importSkillRejectsTooManyFiles() throws IOException {
        properties.getPackage().setMaxFileCount(1);
        MockMultipartFile file = zipUpload(
            "too-many.zip",
            entry("SKILL.md", "# Skill\n"),
            entry("references/one.md", "# One\n")
        );

        assertThrows(IllegalArgumentException.class, () -> service.importSkill(file, "too-many"));
    }

    @Test
    void packagePathRequiresExistingSkill() {
        assertThrows(IllegalArgumentException.class, () -> service.packagePath("missing-skill"));
    }

    private MockMultipartFile zipUpload(String name, ZipTestEntry... entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (ZipTestEntry entry : entries) {
                zos.putNextEntry(new ZipEntry(entry.name()));
                zos.write(entry.content().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return new MockMultipartFile("file", name, "application/zip", out.toByteArray());
    }

    private ZipTestEntry entry(String name, String content) {
        return new ZipTestEntry(name, content);
    }

    private int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private record ZipTestEntry(String name, String content) {
    }
}
