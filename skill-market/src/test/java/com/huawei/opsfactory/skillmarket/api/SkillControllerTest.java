/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.huawei.opsfactory.skillmarket.config.SkillMarketProperties;
import com.huawei.opsfactory.skillmarket.config.WebConfig;
import com.huawei.opsfactory.skillmarket.model.CreateSkillRequest;
import com.huawei.opsfactory.skillmarket.model.SkillDetail;
import com.huawei.opsfactory.skillmarket.model.SkillMutationResponse;
import com.huawei.opsfactory.skillmarket.model.SkillSummary;
import com.huawei.opsfactory.skillmarket.model.SkillWarning;
import com.huawei.opsfactory.skillmarket.model.UpdateSkillRequest;
import com.huawei.opsfactory.skillmarket.service.SkillCatalogService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({SkillController.class, SystemController.class})
@Import({SkillMarketProperties.class, WebConfig.class})
/**
 * Skill Controller Test.
 *
 * @author x00000000
 * @since 2026-05-27
 */
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SkillCatalogService service;

    @TempDir
    Path tempDir;

    @Test
    void listSkillsReturnsItems() throws Exception {
        Mockito.when(service.listSkills("log")).thenReturn(List.of(summary()));

        mockMvc.perform(get("/skill-market/skills").queryParam("q", "log"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value("log-analysis"));
    }

    @Test
    void createSkillAcceptsJsonBody() throws Exception {
        Mockito.when(service.createSkill(any(CreateSkillRequest.class)))
            .thenReturn(new SkillMutationResponse(summary(), List.of()));

        mockMvc.perform(post("/skill-market/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "id": "log-analysis",
                      "name": "Log Analysis",
                      "description": "Analyze logs",
                      "instructions": "# Log Analysis"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.skill.id").value("log-analysis"));
    }

    @Test
    void importSkillAcceptsMultipartUpload() throws Exception {
        Mockito.when(service.importSkill(any(), eq("log-analysis")))
            .thenReturn(new SkillMutationResponse(summary(), List.of(
                new SkillWarning("CONTAINS_SCRIPTS", "Skill package contains executable script files.")
            )));
        MockMultipartFile file = new MockMultipartFile("file", "skill.zip", "application/zip", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/skill-market/skills:import").file(file).param("id", "log-analysis"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.skill.id").value("log-analysis"))
            .andExpect(jsonPath("$.warnings[0].code").value("CONTAINS_SCRIPTS"));
    }

    @Test
    void updateSkillAcceptsJsonBody() throws Exception {
        Mockito.when(service.updateSkill(eq("log-analysis"), any(UpdateSkillRequest.class)))
            .thenReturn(new SkillMutationResponse(summary(), List.of()));

        mockMvc.perform(put("/skill-market/skills/log-analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Log Analysis",
                      "description": "Analyze logs",
                      "instructions": "---\\nname: log-analysis\\n---\\n# Log Analysis"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.skill.id").value("log-analysis"));
    }

    @Test
    void detailReturnsFileList() throws Exception {
        Mockito.when(service.getSkill("log-analysis"))
            .thenReturn(new SkillDetail(
                "log-analysis",
                "Log Analysis",
                "Analyze logs",
                "skills/log-analysis",
                true,
                "sha256:abc",
                10,
                2,
                "SKILL.md",
                List.of("SKILL.md", "scripts/analyze.py"),
                "---\nname: log-analysis\n---\n# Log Analysis\n",
                "2026-04-22T00:00:00Z",
                "2026-04-22T00:00:00Z"
            ));

        mockMvc.perform(get("/skill-market/skills/log-analysis"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.files[1]").value("scripts/analyze.py"))
            .andExpect(jsonPath("$.instructions").value("---\nname: log-analysis\n---\n# Log Analysis\n"));
    }

    @Test
    void downloadPackageReturnsZipAttachment() throws Exception {
        Path zip = tempDir.resolve("log-analysis.zip");
        Files.write(zip, new byte[] {1, 2, 3});
        Mockito.when(service.packagePath("log-analysis")).thenReturn(zip);

        mockMvc.perform(get("/skill-market/skills/log-analysis/package"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/zip"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"log-analysis.zip\""));
    }

    @Test
    void deleteSkillReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/skill-market/skills/log-analysis"))
            .andExpect(status().isNoContent());
    }

    @Test
    void capabilitiesExposePackageLimits() throws Exception {
        mockMvc.perform(get("/skill-market/capabilities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.packageFormats[0]").value("zip"))
            .andExpect(jsonPath("$.limits.maxUploadSizeMb").value(50));
    }

    @Test
    void corsAllowsLocalhostDevOrigin() throws Exception {
        mockMvc.perform(get("/skill-market/capabilities")
                .header("Origin", "http://localhost:5173"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private SkillSummary summary() {
        return new SkillSummary(
            "log-analysis",
            "Log Analysis",
            "Analyze logs",
            "skills/log-analysis",
            true,
            "sha256:abc",
            10,
            2,
            "2026-04-22T00:00:00Z",
            "2026-04-22T00:00:00Z"
        );
    }
}
