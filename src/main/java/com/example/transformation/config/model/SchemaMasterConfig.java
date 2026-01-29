package com.example.transformation.config.model;

import java.util.Map;

/**
 * Model for schema-master.yaml
 * Loaded via Jackson YAML ObjectMapper.
 */
public record SchemaMasterConfig(Map<String, CartridgeSchema> cartridges) {

    public SchemaMasterConfig {
        cartridges = cartridges != null ? Map.copyOf(cartridges) : Map.of();
    }

    public record CartridgeSchema(String description, String provider, String inputFormat) {}
}
