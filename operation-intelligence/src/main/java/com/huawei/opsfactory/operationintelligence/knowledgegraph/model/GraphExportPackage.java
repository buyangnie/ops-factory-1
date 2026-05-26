/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.operationintelligence.knowledgegraph.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Native knowledge graph export package.
 *
 * @author x00000000
 * @since 2026-05-20
 */
public class GraphExportPackage {
    private Map<String, Object> manifest = new LinkedHashMap<>();

    private GraphOntology ontology;

    private String schemaDsl;

    private GraphSnapshot snapshot;

    /**
     * Gets the manifest.
     *
     * @return the result
     */
    public Map<String, Object> getManifest() {
        return manifest;
    }

    /**
     * Sets the manifest.
     *
     * @param manifest the manifest
     */
    public void setManifest(Map<String, Object> manifest) {
        this.manifest = manifest;
    }

    /**
     * Gets the ontology.
     *
     * @return the result
     */
    public GraphOntology getOntology() {
        return ontology;
    }

    /**
     * Sets the ontology.
     *
     * @param ontology the ontology
     */
    public void setOntology(GraphOntology ontology) {
        this.ontology = ontology;
    }

    /**
     * Gets the schema DSL.
     *
     * @return the result
     */
    public String getSchemaDsl() {
        return schemaDsl;
    }

    /**
     * Sets the schema DSL.
     *
     * @param schemaDsl the schemaDsl
     */
    public void setSchemaDsl(String schemaDsl) {
        this.schemaDsl = schemaDsl;
    }

    /**
     * Gets the snapshot.
     *
     * @return the result
     */
    public GraphSnapshot getSnapshot() {
        return snapshot;
    }

    /**
     * Sets the snapshot.
     *
     * @param snapshot the snapshot
     */
    public void setSnapshot(GraphSnapshot snapshot) {
        this.snapshot = snapshot;
    }
}
