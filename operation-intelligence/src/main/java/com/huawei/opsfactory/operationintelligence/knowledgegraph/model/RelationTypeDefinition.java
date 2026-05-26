/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.huawei.opsfactory.operationintelligence.knowledgegraph.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Relation type definition in a graph ontology.
 *
 * @author x00000000
 * @since 2026-05-22
 */
public class RelationTypeDefinition {
    private String type;

    private List<String> from = new ArrayList<>();

    private List<String> to = new ArrayList<>();

    private String layer;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getFrom() {
        return from;
    }

    public void setFrom(List<String> from) {
        this.from = from == null ? new ArrayList<>() : new ArrayList<>(from);
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to == null ? new ArrayList<>() : new ArrayList<>(to);
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }
}
