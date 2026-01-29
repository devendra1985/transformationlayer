package com.example.transformation.processor;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.ErrorCodes;
import com.example.transformation.config.model.ResolvedCartridgeContext;
import com.example.transformation.enrich.EnrichmentConfig;
import com.example.transformation.enrich.EnrichmentEngine;
import com.example.transformation.enrich.EnrichmentLoader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
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
  private final ApplicationContext appContext;
  private final int bulkParallelism;
  private final ForkJoinPool bulkPool;

  public EnrichProcessor(
      EnrichmentLoader enrichmentLoader,
      EnrichmentEngine engine,
      ApplicationContext appContext,
      @Value("${app.bulk.parallelism:0}") int bulkParallelism
  ) {
    this.enrichmentLoader = enrichmentLoader;
    this.engine = engine;
    this.appContext = appContext;
    this.bulkParallelism = bulkParallelism;
    this.bulkPool = (bulkParallelism > 1) ? new ForkJoinPool(bulkParallelism) : null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process(Exchange exchange) {
    // Get resolved context from validate processor
    ResolvedCartridgeContext context = exchange.getProperty(
        ExchangeKeys.RESOLVED_CONTEXT_PROP, ResolvedCartridgeContext.class);

    Object body = exchange.getMessage().getBody();
    if (body instanceof List<?> list) {
      List<BulkRecord> records = (List<BulkRecord>) list;
      runInParallel(records, record -> {
        if (record.hasError()) {
          return;
        }
        Map<String, Object> input = record.getInput();
        if (input == null) {
          record.setError(BulkError.functional("Missing input for enrichment", null, "ENRICHMENT"));
          return;
        }
        try {
          record.setInput(applyEnrichment(context, input));
        } catch (CartridgeException e) {
          record.setError(BulkError.from(e));
        }
      });
      exchange.getMessage().setBody(records);
      return;
    }

    if (!(body instanceof Map<?, ?> m)) {
      return;
    }

    Map<String, Object> copy = new LinkedHashMap<>((Map<String, Object>) m);

    // Cartridge-specific enrichment rules using resolved context
    copy = applyEnrichment(context, copy);

    exchange.getMessage().setBody(copy);
  }

  private void runInParallel(List<BulkRecord> records, java.util.function.Consumer<BulkRecord> work) {
    if (bulkParallelism <= 1) {
      records.forEach(work);
      return;
    }
    try {
      bulkPool.submit(() -> records.parallelStream().forEach(work)).get();
    } catch (Exception e) {
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.GENERIC_TECHNICAL), CartridgeException.ErrorType.TECHNICAL,
          "Bulk parallel enrichment failed", e, null, "ENRICHMENT");
    }
  }

  private Map<String, Object> applyEnrichment(ResolvedCartridgeContext context, Map<String, Object> input) {
    Map<String, Object> copy = new LinkedHashMap<>(input);

    // Cartridge-specific enrichment rules using resolved enrich path
    if (context != null && context.enrichPath() != null) {
      Optional<EnrichmentConfig> cfg = enrichmentLoader.loadOptional(context.enrichPath());
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

    return copy;
  }
}

