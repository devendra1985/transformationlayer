package com.example.transformation.cartridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML model for a cartridge mapping definition.
 *
 * Example:
 * cartridgeId: VISABA-USD
 * output:
 *   type: json
 * validations:
 *   - path: $.msgId
 *     required: true
 * mappings:
 *   - source: $.msgId
 *     target: messageId
 */
public class MappingDefinition {

    public String cartridgeId;
    public Output output = new Output();
    public List<ValidationRule> validations = new ArrayList<>();
    public List<MappingRule> mappings = new ArrayList<>();
    public Map<String, Object> metadata = new LinkedHashMap<>();

    public static class Output {
        /** Output type: json */
        public String type = "json";
    }

    public static class ValidationRule {
        /** JSONPath-like: $.a.b.c */
        public String path;
        public boolean required = false;
        /** Optional condition: only apply when this path equals value. */
        public String whenPath;
        public String whenEquals;
        /** Optional condition: only apply when this path exists. */
        public Boolean whenExists;
        /** Optional: if set, the field must equal this value (string compare). */
        public String equals;
        /** Optional: minimum string length (applied to String.valueOf(value)). */
        public Integer minLength;
        /** Optional: maximum string length (applied to String.valueOf(value)). */
        public Integer maxLength;
        /** Optional: regex pattern (Java Pattern). Applied to String.valueOf(value). */
        public String pattern;
        /** Optional: numeric minimum (applied if value is numeric or numeric string). */
        public Double min;
        /** Optional: numeric maximum (applied if value is numeric or numeric string). */
        public Double max;
    }

    public static class MappingRule {
        /** JSONPath-like: $.a.b.c */
        public String source;
        /** Output field path using dot-notation: a.b.c */
        public String target;
        /** If true and source missing/blank => fail. */
        public boolean required = false;
        /** Default string to use when source missing (and not required). */
        public String defaultValue;
    }
}
