package com.example.transformation.config.model;

import java.util.Map;

/**
 * Model for transformation-flow-master.yaml
 * Loaded via Jackson YAML ObjectMapper.
 */
public record TransformationFlowMasterConfig(Map<String, FlowDefinition> flows) {

    public TransformationFlowMasterConfig {
        flows = flows != null ? Map.copyOf(flows) : Map.of();
    }

    public record FlowDefinition(String from, String to) {}
}
