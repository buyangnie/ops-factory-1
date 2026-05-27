/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.skillmarket.api;

import com.huawei.opsfactory.skillmarket.model.CreateSkillRequest;
import com.huawei.opsfactory.skillmarket.model.SkillDetail;
import com.huawei.opsfactory.skillmarket.model.SkillListResponse;
import com.huawei.opsfactory.skillmarket.model.SkillMutationResponse;
import com.huawei.opsfactory.skillmarket.model.SkillSummary;
import com.huawei.opsfactory.skillmarket.model.UpdateSkillRequest;
import com.huawei.opsfactory.skillmarket.service.SkillCatalogService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for skill catalog operations.
 *
 * @author x00000000
 * @since 2026-05-27
 */
@RestController
@RequestMapping("/skill-market")
public class SkillController {

    private final SkillCatalogService service;

    public SkillController(SkillCatalogService service) {
        this.service = service;
    }

    @GetMapping("/skills")
    public SkillListResponse listSkills(@RequestParam(name = "q", required = false) String query) throws IOException {
        List<SkillSummary> items = service.listSkills(query);
        return new SkillListResponse(items, items.size());
    }

    @PostMapping("/skills")
    public SkillMutationResponse createSkill(@Valid @RequestBody CreateSkillRequest request) throws IOException {
        return service.createSkill(request);
    }

    @PostMapping(value = "/skills:import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SkillMutationResponse importSkill(@ModelAttribute ImportSkillForm form) throws IOException {
        return service.importSkill(form.file(), form.id());
    }

    @PutMapping("/skills/{skillId}")
    public SkillMutationResponse updateSkill(
        @PathVariable String skillId,
        @Valid @RequestBody UpdateSkillRequest request
    ) throws IOException {
        return service.updateSkill(skillId, request);
    }

    @GetMapping("/skills/{skillId}")
    public SkillDetail getSkill(@PathVariable String skillId) throws IOException {
        return service.getSkill(skillId);
    }

    @GetMapping("/skills/{skillId}/package")
    public ResponseEntity<Resource> downloadPackage(@PathVariable String skillId) {
        Path packagePath = service.packagePath(skillId);
        Resource resource = new FileSystemResource(packagePath);
        String filename = skillId + ".zip";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
            .body(resource);
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<Void> deleteSkill(@PathVariable String skillId) throws IOException {
        service.deleteSkill(skillId);
        return ResponseEntity.noContent().build();
    }

    public record ImportSkillForm(
        MultipartFile file,
        String id
    ) {
    }
}
