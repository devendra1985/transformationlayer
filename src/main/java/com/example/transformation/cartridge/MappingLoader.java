package com.example.transformation.cartridge;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * High-performance mapping definition loader with caching.
 * 
 * Performance optimizations:
 * - Uses Jackson YAML (faster than SnakeYAML)
 * - Single shared ObjectMapper (thread-safe)
 * - ConcurrentHashMap cache with computeIfAbsent
 * - Reads directly from InputStream (no String conversion)
 */
@Component
public class MappingLoader {

    // Shared ObjectMapper - thread-safe, reusable
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules();

    private final ResourceLoader resourceLoader;
    private final ConcurrentMap<String, MappingDefinition> cache = new ConcurrentHashMap<>(32);

    public MappingLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Loads mapping definition with O(1) cache lookup after first load.
     */
    public MappingDefinition load(String mappingResourcePath) {
        return cache.computeIfAbsent(mappingResourcePath, this::readYaml);
    }

    private MappingDefinition readYaml(String mappingResourcePath) {
        Resource resource = resourceLoader.getResource(mappingResourcePath);
        if (!resource.exists()) {
            throw new CartridgeException(
                    ErrorCodes.code(ErrorCodes.MAPPING_NOT_FOUND),
                    CartridgeException.ErrorType.FUNCTIONAL,
                    "Mapping YAML not found: " + mappingResourcePath,
                    null, "VALIDATION");
        }

        try (InputStream is = resource.getInputStream()) {
            MappingDefinition def = YAML_MAPPER.readValue(is, MappingDefinition.class);
            if (def == null) {
                throw new CartridgeException(
                        ErrorCodes.code(ErrorCodes.MAPPING_EMPTY),
                        CartridgeException.ErrorType.FUNCTIONAL,
                        "Empty mapping YAML: " + mappingResourcePath,
                        null, "VALIDATION");
            }
            return def;
        } catch (CartridgeException e) {
            throw e;
        } catch (Exception e) {
            throw new CartridgeException(
                    ErrorCodes.code(ErrorCodes.MAPPING_READ_FAILED),
                    CartridgeException.ErrorType.TECHNICAL,
                    "Failed to read mapping YAML: " + mappingResourcePath,
                    e, null, "VALIDATION");
        }
    }

    /**
     * Clears the cache. Useful for testing or hot-reload.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Returns cache size for monitoring.
     */
    public int cacheSize() {
        return cache.size();
    }
}
