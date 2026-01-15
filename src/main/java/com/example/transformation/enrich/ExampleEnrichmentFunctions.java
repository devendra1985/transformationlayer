package com.example.transformation.enrich;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Example enrichment bean you can call from <cartridge>-enrich.yaml via `call:`.
 *
 * Convention used by the enrichment engine:
 * - method signature: Object method(Map<String,Object> body)
 */
@Component("exampleEnrichmentFunctions")
public class ExampleEnrichmentFunctions {
  /**
   * Example: if amount is 50 (number or "50"), change it to 100.
   * Returns a partial map to merge into the body.
   */
  public Map<String, Object> bumpAmount(Map<String, Object> body) {
    Object amount = body.get("amount");
    String s = amount == null ? null : String.valueOf(amount).trim();
    if ("50".equals(s) || "50.0".equals(s)) {
      Map<String, Object> patch = new LinkedHashMap<>();
      patch.put("amount", 100);
      return patch;
    }
    return Map.of();
  }
}

