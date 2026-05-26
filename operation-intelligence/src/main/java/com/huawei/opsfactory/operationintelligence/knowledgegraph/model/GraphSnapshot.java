/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.operationintelligence.knowledgegraph.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full graph snapshot for one environment.
 *
 * @author x00000000
 * @since 2026-05-20
 */
public class GraphSnapshot {
    private String formatVersion = "1.0";

    private String ontologyId;

    private String envCode;

    private String schemaVersion;

    private String sourceSystem;

    private String importMode = "UPSERT";

    private String snapshotId;

    private String generatedAt;

    private Map<String, Object> metadata = new LinkedHashMap<>();

    private List<GraphEntity> entities = new ArrayList<>();

    private List<GraphRelation> relations = new ArrayList<>();

    private List<GraphObservation> observations = new ArrayList<>();

    public String getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }

    public String getOntologyId() {
        return ontologyId;
    }

    public void setOntologyId(String ontologyId) {
        this.ontologyId = ontologyId;
    }

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String envCode) {
        this.envCode = envCode;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getImportMode() {
        return importMode;
    }

    public void setImportMode(String importMode) {
        this.importMode = importMode;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public List<GraphEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<GraphEntity> entities) {
        this.entities = entities == null ? new ArrayList<>() : new ArrayList<>(entities);
    }

    public List<GraphRelation> getRelations() {
        return relations;
    }

    public void setRelations(List<GraphRelation> relations) {
        this.relations = relations == null ? new ArrayList<>() : new ArrayList<>(relations);
    }

    public List<GraphObservation> getObservations() {
        return observations;
    }

    public void setObservations(List<GraphObservation> observations) {
        this.observations = observations == null ? new ArrayList<>() : new ArrayList<>(observations);
    }
}
