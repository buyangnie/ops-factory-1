/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.operationintelligence.knowledgegraph.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity type definition in a graph ontology.
 *
 * @author x00000000
 * @since 2026-05-22
 */
public class EntityTypeDefinition {
    private String type;

    private List<String> requiredProperties = new ArrayList<>();

    private List<String> optionalProperties = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getRequiredProperties() {
        return requiredProperties;
    }

    public void setRequiredProperties(List<String> requiredProperties) {
        this.requiredProperties = requiredProperties == null ? new ArrayList<>() : new ArrayList<>(requiredProperties);
    }

    public List<String> getOptionalProperties() {
        return optionalProperties;
    }

    public void setOptionalProperties(List<String> optionalProperties) {
        this.optionalProperties = optionalProperties == null ? new ArrayList<>() : new ArrayList<>(optionalProperties);
    }
}
