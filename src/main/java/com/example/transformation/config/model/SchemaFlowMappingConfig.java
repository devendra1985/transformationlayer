package com.example.transformation.config.model;

import java.util.Map;

/**
 * Model for schema-flow-mapping.yaml
 * Loaded via Jackson YAML ObjectMapper.
 */
public record SchemaFlowMappingConfig(Map<String, CartridgeFlow> cartridgeFlows) {

    public SchemaFlowMappingConfig {
        cartridgeFlows = cartridgeFlows != null ? Map.copyOf(cartridgeFlows) : Map.of();
    }

    public record CartridgeFlow(FlowDirection outbound, FlowDirection inbound) {}

    public record FlowDirection(String flowId) {}
}
