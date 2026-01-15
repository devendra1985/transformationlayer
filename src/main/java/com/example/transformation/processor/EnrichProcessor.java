package com.example.transformation.processor;

import com.example.transformation.enrich.EnrichmentConfig;
import com.example.transformation.enrich.EnrichmentEngine;
import com.example.transformation.enrich.EnrichmentLoader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Enrichment / enhancement hook.
 *
 * Keep this intentionally simple:
 * - normalize currency to uppercase (if present)
 * - trim bic (if present)
 *
 * Add any derived/default fields here before mapping.
 */
@Component("enrich")
public class EnrichProcessor implements Processor {
  private final EnrichmentLoader enrichmentLoader;
  private final EnrichmentEngine engine;
  private final String basePath;
  private final ApplicationContext appContext;

  public EnrichProcessor(
      EnrichmentLoader enrichmentLoader,
      EnrichmentEngine engine,
      ApplicationContext appContext,
      @Value("${app.cartridges.base-path:classpath:cartridges}") String basePath
  ) {
    this.enrichmentLoader = enrichmentLoader;
    this.engine = engine;
    this.appContext = appContext;
    this.basePath = basePath;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process(Exchange exchange) {
    Object body = exchange.getMessage().getBody();
    if (!(body instanceof Map<?, ?> m)) {
      return;
    }

    Map<String, Object> copy = new LinkedHashMap<>((Map<String, Object>) m);

    // Cartridge-specific enrichment rules (optional file per cartridge)
    String cartridgeId = exchange.getMessage().getHeader(ExchangeKeys.CARTRIDGE_ID_HEADER, String.class);
    if (cartridgeId != null && !cartridgeId.isBlank()) {
      String enrichPath = basePath + "/" + cartridgeId + "/" + cartridgeId + "-enrich.yaml";
      Optional<EnrichmentConfig> cfg = enrichmentLoader.loadOptional(enrichPath);
      if (cfg.isPresent()) {
        copy = engine.apply(copy, cfg.get(), appContext);
      }
    }

    Object currency = copy.get("currency");
    if (currency instanceof String s && !s.isBlank()) {
      copy.put("currency", s.trim().toUpperCase());
    }

    Object bic = copy.get("bic");
    if (bic instanceof String s && !s.isBlank()) {
      copy.put("bic", s.trim());
    }

    exchange.getMessage().setBody(copy);
  }
}

