/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.operationintelligence.controller;

import com.huawei.opsfactory.operationintelligence.knowledgegraph.model.GraphOntology;
import com.huawei.opsfactory.operationintelligence.knowledgegraph.model.GraphSnapshot;
import com.huawei.opsfactory.operationintelligence.knowledgegraph.service.KnowledgeGraphService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Knowledge graph controller.
 *
 * @author x00000000
 * @since 2026-05-20
 */
@RestController
@RequestMapping("/operation-intelligence/graph")
public class KnowledgeGraphController {
    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * Imports or updates ontology data.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/ontologies")
    public Mono<Map<String, Object>> importOntology(@RequestBody GraphOntology request) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.importOntology(request)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Lists ontologies.
     *
     * @return the result
     */
    @GetMapping("/ontologies")
    public Mono<Map<String, Object>> listOntologies() {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.listOntologies()))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Gets ontology detail.
     *
     * @param ontologyId the ontologyId
     * @return the result
     */
    @GetMapping("/ontologies/{ontologyId}")
    public Mono<Map<String, Object>> getOntology(@PathVariable("ontologyId") String ontologyId) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.getOntology(ontologyId)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Lists graph entity environments under one ontology.
     *
     * @param ontologyId the ontologyId
     * @return the result
     */
    @GetMapping("/environments")
    public Mono<Map<String, Object>> listEnvironments(@RequestParam(value = "ontologyId", required = false)
        String ontologyId) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.listEnvironments(ontologyId)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes one ontology and all graph snapshots under it.
     *
     * @param ontologyId the ontologyId
     * @return the result
     */
    @DeleteMapping("/ontologies/{ontologyId}")
    public Mono<Map<String, Object>> deleteOntology(@PathVariable("ontologyId") String ontologyId) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.deleteOntology(ontologyId)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes one ontology and all graph snapshots under it.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/admin/delete-ontology")
    public Mono<Map<String, Object>> deleteOntologyByPost(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            String ontologyId = stringValue(request.get("ontologyId"));
            return ok("result", knowledgeGraphService.deleteOntology(ontologyId));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Imports graph data.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/admin/import")
    public Mono<Map<String, Object>> importGraph(@RequestBody GraphSnapshot request) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.importGraph(request)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes graph entities for one environment.
     *
     * @param ontologyId the ontologyId
     * @param envCode the envCode
     * @return the result
     */
    @DeleteMapping("/admin/entities")
    public Mono<Map<String, Object>> deleteEntities(@RequestParam(value = "ontologyId", required = false)
        String ontologyId, @RequestParam("envCode") String envCode) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.deleteEntities(ontologyId, envCode)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes graph entities for one environment.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/admin/delete-entities")
    public Mono<Map<String, Object>> deleteEntitiesByPost(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            String ontologyId = stringValue(request.get("ontologyId"));
            String envCode = stringValue(request.get("envCode"));
            return ok("result", knowledgeGraphService.deleteEntities(ontologyId, envCode));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Gets an entity.
     *
     * @param entityId the entityId
     * @param envCode the envCode
     * @return the result
     */
    @GetMapping("/entities/{entityId}")
    public Mono<Map<String, Object>> getEntity(@PathVariable("entityId") String entityId,
        @RequestParam("envCode") String envCode,
        @RequestParam(value = "ontologyId", required = false) String ontologyId) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.getEntity(ontologyId, envCode, entityId)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Gets graph resource tree.
     *
     * @param envCode the envCode
     * @return the result
     */
    @GetMapping("/resources/tree")
    public Mono<Map<String, Object>> getResourceTree(@RequestParam("envCode") String envCode,
        @RequestParam(value = "ontologyId", required = false) String ontologyId) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.getResourceTree(ontologyId, envCode)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Queries a subgraph.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/subgraph")
    public Mono<Map<String, Object>> querySubgraph(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            String envCode = stringValue(request.get("envCode"));
            String ontologyId = stringValue(request.get("ontologyId"));
            String entityId = stringValue(request.get("entityId"));
            if (request.containsKey("upstreamHops") || request.containsKey("downstreamHops")) {
                int upstreamHops = request.containsKey("upstreamHops") ? intValue(request.get("upstreamHops")) : 0;
                int downstreamHops = request.containsKey("downstreamHops") ? intValue(request.get("downstreamHops")) : 0;
                return ok("result", knowledgeGraphService.querySubgraph(ontologyId, envCode, entityId, upstreamHops,
                    downstreamHops));
            }
            int maxHops = request.containsKey("maxHops") ? intValue(request.get("maxHops")) : 1;
            return ok("result", knowledgeGraphService.querySubgraph(ontologyId, envCode, entityId, maxHops));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Queries observations.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/observations/query")
    public Mono<Map<String, Object>> queryObservations(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.queryObservations(request)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Finds an impact path.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/impact-path")
    public Mono<Map<String, Object>> findImpactPath(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.findImpactPath(request)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Gets root cause candidates.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/root-cause-candidates")
    public Mono<Map<String, Object>> getRootCauseCandidates(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.getRootCauseCandidates(request)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Gets diagnosis context.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/diagnosis/context")
    public Mono<Map<String, Object>> getDiagnosisContext(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> ok("result", knowledgeGraphService.getDiagnosisContext(request)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Exports native graph data.
     *
     * @param request the request
     * @return the result
     */
    @PostMapping("/admin/export")
    public Mono<Map<String, Object>> exportGraph(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            String envCode = stringValue(request.get("envCode"));
            String ontologyId = stringValue(request.get("ontologyId"));
            return ok("result", knowledgeGraphService.exportGraph(ontologyId, envCode));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Map<String, Object> ok(String key, Object value) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put(key, value);
        response.put("error", null);
        return response;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid integer value: " + value);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid integer value: " + value);
    }
}
