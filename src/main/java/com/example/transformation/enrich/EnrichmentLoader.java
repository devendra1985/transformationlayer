package com.example.transformation.enrich;

import com.example.transformation.cartridge.CartridgeException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Component
public class EnrichmentLoader {
  private final ResourceLoader resourceLoader;
  private final Yaml yaml;
  private final ConcurrentMap<String, Optional<EnrichmentConfig>> cache = new ConcurrentHashMap<>();

  public EnrichmentLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(false);
    this.yaml = new Yaml(new Constructor(EnrichmentConfig.class, options));
  }

  /**
   * Loads an enrichment YAML if it exists, otherwise returns Optional.empty().
   */
  public Optional<EnrichmentConfig> loadOptional(String enrichResourcePath) {
    return cache.computeIfAbsent(enrichResourcePath, this::readYamlOptional);
  }

  private Optional<EnrichmentConfig> readYamlOptional(String enrichResourcePath) {
    Resource resource = resourceLoader.getResource(enrichResourcePath);
    if (!resource.exists()) {
      return Optional.empty();
    }
    try (InputStream is = resource.getInputStream()) {
      EnrichmentConfig cfg = yaml.loadAs(new String(is.readAllBytes(), StandardCharsets.UTF_8), EnrichmentConfig.class);
      if (cfg == null) {
        return Optional.empty();
      }
      return Optional.of(cfg);
    } catch (Exception e) {
      throw new CartridgeException("Failed to read enrichment YAML: " + enrichResourcePath, e);
    }
  }
}

