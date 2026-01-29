package com.example.transformation.cartridge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MappingEngine {
  // Cache compiled regex patterns to avoid recompilation on every validation
  private static final ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>(32);
  private static final ConcurrentHashMap<MappingDefinition, MappingPlan> PLAN_CACHE = new ConcurrentHashMap<>();

  public Map<String, Object> map(Object input, MappingDefinition def) {
    MappingPlan plan = getPlan(def);
    
    // Pre-size map based on number of mappings for better performance
    Map<String, Object> out = new LinkedHashMap<>(Math.max(plan.mappings.size(), 4));

    for (CompiledMappingRule rule : plan.mappings) {
      Object v = JsonPathMini.get(input, rule.source);

      if (v == null || (v instanceof String s && s.isEmpty()) || (v instanceof String s2 && isBlank(s2))) {
        if (rule.required) {
          throw new CartridgeException(ErrorCodes.code(ErrorCodes.MAPPING_SOURCE_MISSING), CartridgeException.ErrorType.FUNCTIONAL,
              "Required mapping source missing: " + rule.source + " -> " + rule.target, rule.source, "TRANSFORM");
        }
        if (rule.defaultValue != null) {
          v = rule.defaultValue;
        } else {
          continue;
        }
      }

      JsonPathMini.put(out, rule.target, v);
    }

    if (!plan.validations.isEmpty()) {
      validate(out, plan.validations, "TRANSFORM");
    }
    return out;
  }

  private void validate(Object input, java.util.List<CompiledValidationRule> rules, String step) {
    if (rules.isEmpty()) {
      return;
    }
    for (CompiledValidationRule v : rules) {
      if (!matchesCondition(input, v)) {
        continue;
      }
      if (v.isArrayPath) {
        validateArrayPath(input, v, step);
        continue;
      }
      Object value = JsonPathMini.get(input, v.path);
      if (v.required) {
        if (value == null || (value instanceof String s && isBlank(s))) {
          throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_REQUIRED), CartridgeException.ErrorType.FUNCTIONAL,
              "Validation failed: required field missing at " + v.path, v.path, step);
        }
      }
      if (v.equals != null) {
        String actual = (value == null) ? null : String.valueOf(value);
        if (!v.equals.equals(actual)) {
          throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_EQUALS), CartridgeException.ErrorType.FUNCTIONAL,
              "Validation failed: " + v.path + " must equal '" + v.equals + "' but was '" + actual + "'", v.path,
              step);
        }
      }

      if (value != null) {
        applyValueChecks(value, v, step);
      }
    }
  }

  private void validateArrayPath(Object input, CompiledValidationRule v, String step) {
    Object listObj = JsonPathMini.get(input, v.arrayPath);
    if (!(listObj instanceof java.util.List<?> list)) {
      if (v.required) {
        throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_REQUIRED), CartridgeException.ErrorType.FUNCTIONAL,
            "Validation failed: required field missing at " + v.path, v.path, step);
      }
      return;
    }
    if (list.isEmpty() && v.required) {
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_REQUIRED), CartridgeException.ErrorType.FUNCTIONAL,
          "Validation failed: required field missing at " + v.path, v.path, step);
    }
    for (Object item : list) {
      Object value;
      if (v.arrayFieldPath == null || v.arrayFieldPath.isEmpty()) {
        value = item;
      } else {
        value = JsonPathMini.get(item, "$." + v.arrayFieldPath);
      }
      if (v.required) {
        if (value == null || (value instanceof String s && isBlank(s))) {
          throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_REQUIRED), CartridgeException.ErrorType.FUNCTIONAL,
              "Validation failed: required field missing at " + v.path, v.path, step);
        }
      }
      if (v.equals != null) {
        String actual = (value == null) ? null : String.valueOf(value);
        if (!v.equals.equals(actual)) {
          throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_EQUALS), CartridgeException.ErrorType.FUNCTIONAL,
              "Validation failed: " + v.path + " must equal '" + v.equals + "' but was '" + actual + "'", v.path,
              step);
        }
      }
      if (value != null) {
        applyValueChecks(value, v, step);
      }
    }
  }

  private boolean matchesCondition(Object input, CompiledValidationRule v) {
    if (v.whenPath == null || v.whenPath.isEmpty()) {
      return true;
    }
    Object condValue = JsonPathMini.get(input, v.whenPath);
    if (Boolean.TRUE.equals(v.whenExists)) {
      if (condValue == null) return false;
      if (condValue instanceof String s && isBlank(s)) return false;
    }
    if (v.whenEquals != null) {
      String actual = (condValue == null) ? null : String.valueOf(condValue);
      return v.whenEquals.equals(actual);
    }
    return true;
  }

  private void applyValueChecks(Object value, CompiledValidationRule v, String step) {
    if (v.minLength != null || v.maxLength != null || v.pattern != null) {
      String s = String.valueOf(value);
      int len = s.length();
      if (v.minLength != null && len < v.minLength) {
        throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_MIN_LENGTH), CartridgeException.ErrorType.FUNCTIONAL,
            "Validation failed: " + v.path + " length must be >= " + v.minLength, v.path, step);
      }
      if (v.maxLength != null && len > v.maxLength) {
        throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_MAX_LENGTH), CartridgeException.ErrorType.FUNCTIONAL,
            "Validation failed: " + v.path + " length must be <= " + v.maxLength, v.path, step);
      }
      if (v.pattern != null) {
        Pattern p = (v.compiledPattern != null) ? v.compiledPattern : getCompiledPattern(v.pattern);
        if (!p.matcher(s).matches()) {
          throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_PATTERN), CartridgeException.ErrorType.FUNCTIONAL,
              "Validation failed: " + v.path + " must match pattern " + v.pattern, v.path, step);
        }
      }
    }

    if (v.min != null || v.max != null) {
      Double n = toNumberOrNull(value);
      if (n == null) {
        throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_NUMBER), CartridgeException.ErrorType.FUNCTIONAL,
            "Validation failed: " + v.path + " must be a number", v.path, step);
      }
      double nv = n;
      if (v.min != null && nv < v.min) {
        throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_MIN), CartridgeException.ErrorType.FUNCTIONAL,
            "Validation failed: " + v.path + " must be >= " + v.min, v.path, step);
      }
      if (v.max != null && nv > v.max) {
        throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_MAX), CartridgeException.ErrorType.FUNCTIONAL,
            "Validation failed: " + v.path + " must be <= " + v.max, v.path, step);
      }
    }
  }

  private MappingPlan getPlan(MappingDefinition def) {
    return PLAN_CACHE.computeIfAbsent(def, MappingEngine::compilePlan);
  }

  private static MappingPlan compilePlan(MappingDefinition def) {
    MappingPlan plan = new MappingPlan();
    if (def.mappings != null) {
      for (MappingDefinition.MappingRule rule : def.mappings) {
        plan.mappings.add(new CompiledMappingRule(rule));
      }
    }
    if (def.validations != null) {
      for (MappingDefinition.ValidationRule v : def.validations) {
        plan.validations.add(new CompiledValidationRule(v));
      }
    }
    return plan;
  }

  private static class MappingPlan {
    final java.util.List<CompiledMappingRule> mappings = new java.util.ArrayList<>();
    final java.util.List<CompiledValidationRule> validations = new java.util.ArrayList<>();
  }

  private static class CompiledMappingRule {
    final String source;
    final String target;
    final boolean required;
    final String defaultValue;

    CompiledMappingRule(MappingDefinition.MappingRule rule) {
      this.source = rule.source;
      this.target = rule.target;
      this.required = rule.required;
      this.defaultValue = rule.defaultValue;
    }
  }

  private static class CompiledValidationRule {
    final String path;
    final boolean required;
    final String whenPath;
    final String whenEquals;
    final Boolean whenExists;
    final String equals;
    final Integer minLength;
    final Integer maxLength;
    final String pattern;
    final Pattern compiledPattern;
    final Double min;
    final Double max;
    final boolean isArrayPath;
    final String arrayPath;
    final String arrayFieldPath;

    CompiledValidationRule(MappingDefinition.ValidationRule v) {
      this.path = v.path;
      this.required = v.required;
      this.whenPath = v.whenPath;
      this.whenEquals = v.whenEquals;
      this.whenExists = v.whenExists;
      this.equals = v.equals;
      this.minLength = v.minLength;
      this.maxLength = v.maxLength;
      this.pattern = v.pattern;
      this.compiledPattern = (v.pattern == null) ? null : getCompiledPattern(v.pattern);
      this.min = v.min;
      this.max = v.max;
      String[] arrayInfo = parseArrayPath(v.path);
      this.isArrayPath = arrayInfo != null;
      this.arrayPath = (arrayInfo == null) ? null : arrayInfo[0];
      this.arrayFieldPath = (arrayInfo == null) ? null : arrayInfo[1];
    }
  }

  private static String[] parseArrayPath(String path) {
    if (path == null) return null;
    int idx = path.indexOf("[]");
    if (idx < 0) return null;
    String listPath = path.substring(0, idx);
    String fieldPath = "";
    if (idx + 2 < path.length()) {
      String rest = path.substring(idx + 2);
      if (rest.startsWith(".")) {
        fieldPath = rest.substring(1);
      } else {
        fieldPath = rest;
      }
    }
    return new String[] { listPath, fieldPath };
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
  }
}

