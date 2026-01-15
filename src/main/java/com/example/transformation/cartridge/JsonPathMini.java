package com.example.transformation.cartridge;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal JSONPath-ish accessor with optimized path parsing.
 *
 * Supported:
 * - $.a.b.c (maps)
 * - $.a.0.b (lists by numeric segment)
 *
 * Not supported: filters, wildcards, predicates.
 *
 * Performance optimizations:
 * - Pre-parsed path segments are cached
 * - Integer indices are pre-parsed for list access
 * - No regex compilation at runtime
 */
public final class JsonPathMini {
  private JsonPathMini() {}

  // Cache for parsed JSONPath segments (source paths like $.a.b.c)
  private static final ConcurrentHashMap<String, ParsedPath> SOURCE_PATH_CACHE = new ConcurrentHashMap<>(64);
  
  // Cache for parsed dot-path segments (target paths like a.b.c)
  private static final ConcurrentHashMap<String, String[]> TARGET_PATH_CACHE = new ConcurrentHashMap<>(64);

  /**
   * Pre-parsed path representation for fast traversal.
   */
  private static final class ParsedPath {
    final String[] segments;
    final int[] indices; // Pre-parsed integer indices, -1 if not numeric

    ParsedPath(String[] segments) {
      this.segments = segments;
      this.indices = new int[segments.length];
      for (int i = 0; i < segments.length; i++) {
        this.indices[i] = parseIndex(segments[i]);
      }
    }

    private static int parseIndex(String seg) {
      if (seg.isEmpty()) return -1;
      // Fast path: check if all chars are digits
      for (int i = 0; i < seg.length(); i++) {
        char c = seg.charAt(i);
        if (c < '0' || c > '9') return -1;
      }
      try {
        return Integer.parseInt(seg);
      } catch (NumberFormatException e) {
        return -1;
      }
    }
  }

  private static ParsedPath parsePath(String path) {
    return SOURCE_PATH_CACHE.computeIfAbsent(path, JsonPathMini::doParseSourcePath);
  }

  private static ParsedPath doParseSourcePath(String path) {
    if (path == null || path.isEmpty()) {
      return new ParsedPath(new String[0]);
    }
    String p = path.charAt(0) == ' ' ? path.trim() : path;
    if ("$".equals(p)) {
      return new ParsedPath(new String[0]);
    }
    if (p.length() < 3 || p.charAt(0) != '$' || p.charAt(1) != '.') {
      throw new IllegalArgumentException("Only paths starting with '$.' are supported. Got: " + path);
    }
    // Manual split for performance (avoids regex)
    return new ParsedPath(splitByDot(p, 2));
  }

  private static String[] parseTargetPath(String dotPath) {
    return TARGET_PATH_CACHE.computeIfAbsent(dotPath, JsonPathMini::doParseTargetPath);
  }

  private static String[] doParseTargetPath(String dotPath) {
    if (dotPath == null || dotPath.isEmpty()) {
      return new String[0];
    }
    return splitByDot(dotPath, 0);
  }

  /**
   * Split string by '.' starting from given offset. Avoids regex overhead.
   */
  private static String[] splitByDot(String s, int startOffset) {
    // Count dots first
    int dotCount = 0;
    for (int i = startOffset; i < s.length(); i++) {
      if (s.charAt(i) == '.') dotCount++;
    }
    
    String[] result = new String[dotCount + 1];
    int segStart = startOffset;
    int segIdx = 0;
    
    for (int i = startOffset; i < s.length(); i++) {
      if (s.charAt(i) == '.') {
        result[segIdx++] = s.substring(segStart, i);
        segStart = i + 1;
      }
    }
    result[segIdx] = s.substring(segStart);
    return result;
  }

  public static Object get(Object root, String path) {
    if (path == null || path.isEmpty()) {
      return null;
    }
    // Fast path for blank check without creating new string
    if (isBlank(path)) {
      return null;
    }
    
    ParsedPath parsed = parsePath(path);
    if (parsed.segments.length == 0) {
      return root;
    }
    
    Object cur = root;
    String[] segments = parsed.segments;
    int[] indices = parsed.indices;
    
    for (int i = 0; i < segments.length; i++) {
      if (cur == null) {
        return null;
      }
      if (cur instanceof Map<?, ?> m) {
        cur = m.get(segments[i]);
      } else if (cur instanceof List<?> list) {
        int idx = indices[i];
        if (idx < 0) {
          // Not a valid index - try map lookup failed
          return null;
        }
        cur = (idx < list.size()) ? list.get(idx) : null;
      } else {
        return null;
      }
    }
    return cur;
  }

  @SuppressWarnings("unchecked")
  public static void put(Map<String, Object> root, String dotPath, Object value) {
    if (dotPath == null || dotPath.isEmpty()) {
      return;
    }
    
    String[] segments = parseTargetPath(dotPath);
    if (segments.length == 0) {
      return;
    }
    
    Map<String, Object> cur = root;
    int lastIdx = segments.length - 1;
    
    for (int i = 0; i < lastIdx; i++) {
      String seg = segments[i];
      Object existing = cur.get(seg);
      if (existing instanceof Map<?, ?>) {
        cur = (Map<String, Object>) existing;
      } else {
        Map<String, Object> next = new java.util.LinkedHashMap<>(4);
        cur.put(seg, next);
        cur = next;
      }
    }
    cur.put(segments[lastIdx], value);
  }

  private static boolean isBlank(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) > ' ') return false;
    }
    return true;
  }
}

