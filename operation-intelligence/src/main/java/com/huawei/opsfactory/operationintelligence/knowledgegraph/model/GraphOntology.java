/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.operationintelligence.knowledgegraph.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Knowledge graph ontology definition.
 *
 * @author x00000000
 * @since 2026-05-22
 */
public class GraphOntology {
    private String ontologyId;

    private String name;

    private String version = "1.0";

    private String sourceSystem;

    private Map<String, Object> metadata = new LinkedHashMap<>();

    private List<EntityTypeDefinition> entityTypes = new ArrayList<>();

    private List<RelationTypeDefinition> relationTypes = new ArrayList<>();

    public String getOntologyId() {
        return ontologyId;
    }

    public void setOntologyId(String ontologyId) {
        this.ontologyId = ontologyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public List<EntityTypeDefinition> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(List<EntityTypeDefinition> entityTypes) {
        this.entityTypes = entityTypes == null ? new ArrayList<>() : new ArrayList<>(entityTypes);
    }

    public List<RelationTypeDefinition> getRelationTypes() {
        return relationTypes;
    }

    public void setRelationTypes(List<RelationTypeDefinition> relationTypes) {
        this.relationTypes = relationTypes == null ? new ArrayList<>() : new ArrayList<>(relationTypes);
    }
}
