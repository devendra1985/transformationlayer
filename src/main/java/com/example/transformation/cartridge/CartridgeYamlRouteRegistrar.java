package com.example.transformation.cartridge;

import java.io.InputStream;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.support.ResourceHelper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Ensures cartridge YAML routes are loaded at runtime.
 *
 * We intentionally do not rely on Spring Boot property-based route discovery here,
 * because it's easy to misconfigure and then "direct:" endpoints won't exist.
 *
 * Plug-and-play rule remains:
 * - add `cartridges/<id>/<id>-route.yaml` and `cartridges/<id>/<id>-mapping.yaml`
 * - the route must start with `direct:<id>`
 */
@Component
public class CartridgeYamlRouteRegistrar implements ApplicationRunner {
  private final CamelContext camelContext;

  public CartridgeYamlRouteRegistrar(CamelContext camelContext) {
    this.camelContext = camelContext;
  }

  @Override
  @SuppressWarnings("resource")
  public void run(ApplicationArguments args) throws Exception {
    Resource[] cartridgeResources = new PathMatchingResourcePatternResolver()
        .getResources("classpath*:cartridges/*/*-route.yaml");
    Resource[] sharedResources = new PathMatchingResourcePatternResolver()
        .getResources("classpath*:routes/*.yaml");

    if (cartridgeResources.length == 0 && sharedResources.length == 0) {
      return;
    }

    YamlRoutesBuilderLoader loader = new YamlRoutesBuilderLoader();
    loader.setCamelContext(camelContext);
    loader.start();
    try {
    for (Resource r : concat(sharedResources, cartridgeResources)) {
        try (InputStream is = r.getInputStream()) {
          byte[] bytes = is.readAllBytes();
          org.apache.camel.spi.Resource camelResource = ResourceHelper.fromBytes(r.getDescription(), bytes);
          RoutesBuilder rb = (RoutesBuilder) loader.loadRoutesBuilder(camelResource);
          camelContext.addRoutes(rb);
        }
      }
    } finally {
      loader.stop();
    }
  }

  private static Resource[] concat(Resource[] a, Resource[] b) {
    Resource[] out = new Resource[a.length + b.length];
    System.arraycopy(a, 0, out, 0, a.length);
    System.arraycopy(b, 0, out, a.length, b.length);
    return out;
  }
}

