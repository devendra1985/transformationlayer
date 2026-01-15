package com.example.transformation.cartridge;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MappingEngine {
  private final XmlMapper xmlMapper = new XmlMapper();
  
  // Cache compiled regex patterns to avoid recompilation on every validation
  private static final ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>(32);

  public Result transform(Object input, MappingDefinition def) {
    validate(input, def);
    
    // Pre-size map based on number of mappings for better performance
    Map<String, Object> out = new LinkedHashMap<>(Math.max(def.mappings.size(), 4));

    for (MappingDefinition.MappingRule rule : def.mappings) {
      Object v = JsonPathMini.get(input, rule.source);

      if (v == null || (v instanceof String s && s.isEmpty()) || (v instanceof String s2 && isBlank(s2))) {
        if (rule.required) {
          throw new CartridgeException("Required mapping source missing: " + rule.source + " -> " + rule.target);
        }
        if (rule.defaultValue != null) {
          v = rule.defaultValue;
        } else {
          continue;
        }
      }

      JsonPathMini.put(out, rule.target, v);
    }

    String type = (def.output != null && def.output.type != null) ? def.output.type : "json";
    if ("xml".equalsIgnoreCase(type)) {
      String root = (def.output.root != null && !def.output.root.isEmpty() && !isBlank(def.output.root)) 
          ? def.output.root : "message";
      try {
        String xml = xmlMapper.writer().withRootName(root).writeValueAsString(out);
        return Result.xml(xml);
      } catch (Exception e) {
        throw new CartridgeException("Failed to serialize XML output", e);
      }
    }

    return Result.json(out);
  }

  private void validate(Object input, MappingDefinition def) {
    if (def.validations == null || def.validations.isEmpty()) {
      return;
    }
    for (MappingDefinition.ValidationRule v : def.validations) {
      Object value = JsonPathMini.get(input, v.path);
      if (v.required) {
        if (value == null || (value instanceof String s && isBlank(s))) {
          throw new CartridgeException("Validation failed: required field missing at " + v.path);
        }
      }
      if (v.equals != null) {
        String actual = (value == null) ? null : String.valueOf(value);
        if (!v.equals.equals(actual)) {
          throw new CartridgeException("Validation failed: " + v.path + " must equal '" + v.equals + "' but was '" + actual + "'");
        }
      }

      if (value != null) {
        // String validations
        if (v.minLength != null || v.maxLength != null || v.pattern != null) {
          String s = String.valueOf(value);
          int len = s.length();
          if (v.minLength != null && len < v.minLength) {
            throw new CartridgeException("Validation failed: " + v.path + " length must be >= " + v.minLength);
          }
          if (v.maxLength != null && len > v.maxLength) {
            throw new CartridgeException("Validation failed: " + v.path + " length must be <= " + v.maxLength);
          }
          if (v.pattern != null) {
            Pattern p = getCompiledPattern(v.pattern);
            if (!p.matcher(s).matches()) {
              throw new CartridgeException("Validation failed: " + v.path + " must match pattern " + v.pattern);
            }
          }
        }

        // Numeric validations
        if (v.min != null || v.max != null) {
          Double n = toNumberOrNull(value);
          if (n == null) {
            throw new CartridgeException("Validation failed: " + v.path + " must be a number");
          }
          double nv = n; // Unbox once
          if (v.min != null && nv < v.min) {
            throw new CartridgeException("Validation failed: " + v.path + " must be >= " + v.min);
          }
          if (v.max != null && nv > v.max) {
            throw new CartridgeException("Validation failed: " + v.path + " must be <= " + v.max);
          }
        }
      }
    }
  }

  /**
   * Get cached compiled pattern, compiling only once per unique pattern string.
   */
  private static Pattern getCompiledPattern(String regex) {
    return PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
  }

  private static Double toNumberOrNull(Object value) {
    if (value instanceof Number n) {
      return n.doubleValue();
    }
    if (value instanceof String s) {
      int len = s.length();
      if (len == 0) return null;
      
      // Fast trim check - avoid creating new string if not needed
      int start = 0;
      int end = len;
      while (start < end && s.charAt(start) <= ' ') start++;
      while (end > start && s.charAt(end - 1) <= ' ') end--;
      if (start == end) return null;
      
      String t = (start == 0 && end == len) ? s : s.substring(start, end);
      try {
        return Double.parseDouble(t);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
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

  public record Result(String contentType, Object body) {
    public static Result json(Object body) {
      return new Result("application/json", body);
    }

    public static Result xml(String xml) {
      return new Result("application/xml", xml);
    }
  }
}

