package com.example.transformation.enrich;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.ErrorCodes;
import com.example.transformation.cartridge.JsonPathMini;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Engine for applying enrichment rules with optimized method caching.
 */
@Component
public class EnrichmentEngine {
  
  // Cache for reflected methods: "className#methodName" -> Method
  private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>(32);
  
  // Pre-computed constant values
  private static final String TOKEN_NOW = "${now}";
  private static final String TOKEN_UUID = "${uuid}";

  /**
   * Returns a new Map with enrichment applied (does not mutate input).
   */
  public Map<String, Object> apply(Map<String, Object> input, EnrichmentConfig cfg, ApplicationContext appContext) {
    if (cfg == null || cfg.rules == null || cfg.rules.isEmpty()) {
      return new LinkedHashMap<>(input);
    }
    
    Map<String, Object> out = new LinkedHashMap<>(input.size() + cfg.rules.size());
    out.putAll(input);

    for (EnrichmentConfig.Rule rule : cfg.rules) {
      if (rule == null) continue;
      if (!matches(out, rule.when)) {
        continue;
      }

      if (rule.set != null) {
        Object v = resolveValue(rule.set.value);
        JsonPathMini.put(out, rule.set.target, v);
      } else if (rule.copy != null) {
        Object v = JsonPathMini.get(out, rule.copy.source);
        JsonPathMini.put(out, rule.copy.target, v);
      } else if (rule.call != null) {
        applyCall(out, rule.call, appContext);
      } else {
        throw new CartridgeException(ErrorCodes.code(ErrorCodes.ENRICH_RULE_INVALID), CartridgeException.ErrorType.FUNCTIONAL,
            "Invalid enrichment rule: must contain 'set' or 'copy' or 'call'", null, "ENRICHMENT");
      }
    }

    return out;
  }

  @SuppressWarnings("unchecked")
  private void applyCall(Map<String, Object> body, EnrichmentConfig.Call call, ApplicationContext appContext) {
    if (appContext == null) {
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.ENRICH_APP_CONTEXT_MISSING), CartridgeException.ErrorType.TECHNICAL,
          "Enrichment call requires ApplicationContext but it was null", null, "ENRICHMENT");
    }
    if (call.bean == null || call.bean.isEmpty() || call.method == null || call.method.isEmpty()) {
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.ENRICH_CALL_MISSING), CartridgeException.ErrorType.FUNCTIONAL,
          "Enrichment call must specify bean and method", null, "ENRICHMENT");
    }

    Object bean = appContext.getBean(call.bean);
    try {
      Method method = getCachedMethod(bean.getClass(), call.method);
      Object result = method.invoke(bean, body);
      
      if (call.target != null && !call.target.isEmpty()) {
        JsonPathMini.put(body, call.target, result);
        return;
      }
      if (result instanceof Map<?, ?> m) {
        body.putAll((Map<String, Object>) m);
        return;
      }
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.ENRICH_CALL_NOT_MAP), CartridgeException.ErrorType.FUNCTIONAL,
          "Enrichment call returned non-Map but no target was provided", null, "ENRICHMENT");
    } catch (CartridgeException e) {
      throw e;
    } catch (Exception e) {
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.ENRICH_CALL_FAILED), CartridgeException.ErrorType.TECHNICAL,
          "Failed enrichment call: " + call.bean + "." + call.method + "(Map)", e, null, "ENRICHMENT");
    }
  }

  /**
   * Get cached Method object to avoid repeated reflection lookups.
   */
  private static Method getCachedMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
    String key = clazz.getName() + "#" + methodName;
    Method cached = METHOD_CACHE.get(key);
    if (cached != null) {
      return cached;
    }
    
    Method method = clazz.getMethod(methodName, Map.class);
    METHOD_CACHE.put(key, method);
    return method;
  }

  private static boolean matches(Map<String, Object> current, EnrichmentConfig.When when) {
    if (when == null) {
      return true;
    }
    Object v = JsonPathMini.get(current, when.path);

    if (Boolean.TRUE.equals(when.exists)) {
      if (v == null) return false;
      if (v instanceof String s && isBlank(s)) return false;
    }

    if (when.equals != null) {
      String actual = (v == null) ? null : String.valueOf(v);
      return when.equals.equals(actual);
    }

    return true;
  }

  private static Object resolveValue(Object value) {
    if (value instanceof String s) {
      // Use reference equality first for common case (interned strings)
      if (TOKEN_NOW.equals(s)) {
        return Instant.now().toString();
      }
      if (TOKEN_UUID.equals(s)) {
        return UUID.randomUUID().toString();
      }
    }
    return value;
  }

  /**
   * Fast blank check without creating new string objects.
   */
  private static boolean isBlank(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) > ' ') return false;
    }
    return true;
  }
}

