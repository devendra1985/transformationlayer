package com.example.transformation.cartridge;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Component
public class MappingLoader {
  private final ResourceLoader resourceLoader;
  private final Yaml yaml;
  private final ConcurrentMap<String, MappingDefinition> cache = new ConcurrentHashMap<>();

  public MappingLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(false);
    this.yaml = new Yaml(new Constructor(MappingDefinition.class, options));
  }

  public MappingDefinition load(String mappingResourcePath) {
    return cache.computeIfAbsent(mappingResourcePath, this::readYaml);
  }

  private MappingDefinition readYaml(String mappingResourcePath) {
    Resource resource = resourceLoader.getResource(mappingResourcePath);
    if (!resource.exists()) {
      throw new CartridgeException("Mapping YAML not found: " + mappingResourcePath);
    }
    try (InputStream is = resource.getInputStream()) {
      MappingDefinition def = yaml.loadAs(new String(is.readAllBytes(), StandardCharsets.UTF_8), MappingDefinition.class);
      if (def == null) {
        throw new CartridgeException("Empty mapping YAML: " + mappingResourcePath);
      }
      return def;
    } catch (Exception e) {
      throw new CartridgeException("Failed to read mapping YAML: " + mappingResourcePath, e);
    }
  }
}

