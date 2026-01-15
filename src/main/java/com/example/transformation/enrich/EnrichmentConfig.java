package com.example.transformation.enrich;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML model for per-cartridge enrichment rules.
 *
 * Example:
 * cartridgeId: cjson-to-rest
 * rules:
 *   - set:
 *       target: channel
 *       value: MOBILE
 *   - when:
 *       path: $.amount
 *       equals: "50"
 *     set:
 *       target: amount
 *       value: 100
 */
public class EnrichmentConfig {
  public String cartridgeId;
  public List<Rule> rules = new ArrayList<>();

  public static class Rule {
    public When when;
    public Set set;
    public Copy copy;
    public Call call;
  }

  public static class When {
    /** JSONPath-like: $.a.b.c */
    public String path;
    /** String compare against String.valueOf(value). */
    public String equals;
    /** If present, the field must exist and be non-blank (for strings). */
    public Boolean exists;
  }

  public static class Set {
    /** Output field path using dot-notation: a.b.c */
    public String target;
    /**
     * Static value to set.
     * Special tokens supported:
     * - ${now}  -> ISO-8601 timestamp
     * - ${uuid} -> random UUID
     */
    public Object value;
  }

  public static class Copy {
    /** JSONPath-like: $.a.b.c */
    public String source;
    /** Output field path using dot-notation: a.b.c */
    public String target;
  }

  public static class Call {
    /** Spring bean name */
    public String bean;
    /** Method name */
    public String method;
    /**
     * Optional: where to put the return value.
     * If omitted and return type is Map -> merge keys into body.
     */
    public String target;
  }
}

