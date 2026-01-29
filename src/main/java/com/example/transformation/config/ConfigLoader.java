package com.example.transformation.config;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.ErrorCodes;
import com.example.transformation.config.model.CartridgeMasterConfig;
import com.example.transformation.config.model.SchemaFlowMappingConfig;
import com.example.transformation.config.model.SchemaMasterConfig;
import com.example.transformation.config.model.TransformationFlowMasterConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Loads and caches all configuration YAML files at startup using Jackson YAML.
 * Jackson provides cleaner record support and better error messages than SnakeYAML.
 */
@Component
public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    // Reusable ObjectMapper for YAML - thread-safe
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .findAndRegisterModules();

    private final ResourceLoader resourceLoader;
    private final String configBasePath;

    private volatile CartridgeMasterConfig cartridgeMasterConfig;
    private volatile SchemaMasterConfig schemaMasterConfig;
    private volatile SchemaFlowMappingConfig schemaFlowMappingConfig;
    private volatile TransformationFlowMasterConfig transformationFlowMasterConfig;

    public ConfigLoader(
            ResourceLoader resourceLoader,
            @Value("${app.config.base-path:classpath:config}") String configBasePath) {
        this.resourceLoader = resourceLoader;
        this.configBasePath = configBasePath;
    }

    @PostConstruct
    public void init() {
        log.info("Loading configuration files from: {}", configBasePath);
        loadAllConfigs();
        logConfigSummary();
        log.info("Configuration files loaded and cached successfully");
    }

    private void loadAllConfigs() {
        cartridgeMasterConfig = loadConfig("cartridge-master.yaml", CartridgeMasterConfig.class);
        schemaMasterConfig = loadConfig("schema-master.yaml", SchemaMasterConfig.class);
        schemaFlowMappingConfig = loadConfig("schema-flow-mapping.yaml", SchemaFlowMappingConfig.class);
        transformationFlowMasterConfig = loadConfig("transformation-flow-master.yaml", TransformationFlowMasterConfig.class);
    }

    private <T> T loadConfig(String filename, Class<T> targetType) {
        String resourcePath = configBasePath + "/" + filename;
        Resource resource = resourceLoader.getResource(resourcePath);

        if (!resource.exists()) {
            throw new CartridgeException(
                    ErrorCodes.code(ErrorCodes.CONFIG_NOT_FOUND),
                    CartridgeException.ErrorType.TECHNICAL,
                    "Configuration file not found: " + resourcePath,
                    null, "CONFIG");
        }

        try (InputStream is = resource.getInputStream()) {
            T config = YAML_MAPPER.readValue(is, targetType);
            if (config == null) {
                throw new CartridgeException(
                        ErrorCodes.code(ErrorCodes.CONFIG_EMPTY),
                        CartridgeException.ErrorType.TECHNICAL,
                        "Empty configuration file: " + resourcePath,
                        null, "CONFIG");
            }
            log.debug("Loaded: {}", filename);
            return config;
        } catch (CartridgeException e) {
            throw e;
        } catch (IOException e) {
            throw new CartridgeException(
                    ErrorCodes.code(ErrorCodes.CONFIG_READ_FAILED),
                    CartridgeException.ErrorType.TECHNICAL,
                    "Failed to read configuration file: " + resourcePath,
                    e, null, "CONFIG");
        }
    }

    private void logConfigSummary() {
        log.info("Loaded {} providers from cartridge-master", cartridgeMasterConfig.providers().size());
        log.info("Loaded {} cartridge schemas from schema-master", schemaMasterConfig.cartridges().size());
        log.info("Loaded {} cartridge flows from schema-flow-mapping", schemaFlowMappingConfig.cartridgeFlows().size());
        log.info("Loaded {} transformation flows from transformation-flow-master", transformationFlowMasterConfig.flows().size());
    }

    public CartridgeMasterConfig getCartridgeMasterConfig() { return cartridgeMasterConfig; }
    public SchemaMasterConfig getSchemaMasterConfig() { return schemaMasterConfig; }
    public SchemaFlowMappingConfig getSchemaFlowMappingConfig() { return schemaFlowMappingConfig; }
    public TransformationFlowMasterConfig getTransformationFlowMasterConfig() { return transformationFlowMasterConfig; }

    public synchronized void reload() {
        log.info("Reloading configuration files...");
        loadAllConfigs();
        log.info("Configuration files reloaded successfully");
    }
}
