package com.example.transformation.cartridge;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ErrorCodes {

    // Generic errors
    public static final String GENERIC_FUNCTIONAL = "generic.functional";
    public static final String GENERIC_TECHNICAL = "generic.technical";

    // Mapping errors
    public static final String MAPPING_NOT_FOUND = "mapping.notFound";
    public static final String MAPPING_EMPTY = "mapping.empty";
    public static final String MAPPING_READ_FAILED = "mapping.readFailed";
    public static final String MAPPING_DEFINITION_MISSING = "mapping.definitionMissing";
    public static final String MAPPING_SOURCE_MISSING = "mapping.sourceMissing";

    // Request errors
    public static final String REQUEST_BODY_TYPE = "request.bodyType";
    public static final String REQUEST_CARTRIDGE_ID_MISSING = "request.cartridgeIdMissing";

    // Enrichment errors
    public static final String ENRICH_RULE_INVALID = "enrich.ruleInvalid";
    public static final String ENRICH_APP_CONTEXT_MISSING = "enrich.appContextMissing";
    public static final String ENRICH_CALL_MISSING = "enrich.callMissing";
    public static final String ENRICH_CALL_NOT_MAP = "enrich.callNotMap";
    public static final String ENRICH_CALL_FAILED = "enrich.callFailed";
    public static final String ENRICH_READ_FAILED = "enrich.readFailed";

    // Validation errors
    public static final String VALIDATION_REQUIRED = "validation.required";
    public static final String VALIDATION_EQUALS = "validation.equals";
    public static final String VALIDATION_MIN_LENGTH = "validation.minLength";
    public static final String VALIDATION_MAX_LENGTH = "validation.maxLength";
    public static final String VALIDATION_PATTERN = "validation.pattern";
    public static final String VALIDATION_NUMBER = "validation.number";
    public static final String VALIDATION_MIN = "validation.min";
    public static final String VALIDATION_MAX = "validation.max";

    // Config errors
    public static final String CONFIG_NOT_FOUND = "config.notFound";
    public static final String CONFIG_EMPTY = "config.empty";
    public static final String CONFIG_READ_FAILED = "config.readFailed";

    // Cartridge resolution errors
    public static final String CARTRIDGE_NOT_FOUND = "cartridge.notFound";
    public static final String CARTRIDGE_FLOW_NOT_FOUND = "cartridge.flowNotFound";
    public static final String CARTRIDGE_TEMPLATE_NOT_FOUND = "cartridge.templateNotFound";

    // Output errors
    public static final String OUTPUT_SERIALIZE_FAILED = "output.serializeFailed";

    private static final String CODES_FILE = "error-codes.properties";
    private static final Properties CODES = new Properties();

    static {
        try (InputStream is = ErrorCodes.class.getClassLoader().getResourceAsStream(CODES_FILE)) {
            if (is == null) {
                throw new IllegalStateException("Missing " + CODES_FILE + " on classpath");
            }
            CODES.load(is);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ErrorCodes() {}

    public static String code(String key) {
        String value = CODES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing error code for key: " + key);
        }
        return value;
    }
}
