package com.example.transformation.config.model;

import java.util.List;
import java.util.Map;

/**
 * Model for cartridge-master.yaml
 * Loaded via Jackson YAML ObjectMapper.
 */
public record CartridgeMasterConfig(Map<String, ProviderConfig> providers) {

    public CartridgeMasterConfig {
        providers = providers != null ? Map.copyOf(providers) : Map.of();
    }

    public record ProviderConfig(String name, List<String> cartridges) {
        public ProviderConfig {
            cartridges = cartridges != null ? List.copyOf(cartridges) : List.of();
        }
    }
}
