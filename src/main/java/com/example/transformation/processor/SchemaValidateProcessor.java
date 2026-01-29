package com.example.transformation.processor;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.ErrorCodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("schemaValidate")
public class SchemaValidateProcessor implements Processor {
  private final ObjectMapper objectMapper;
  private final String schemaPath;
  private volatile JsonSchema schema;

  public SchemaValidateProcessor(
      ObjectMapper objectMapper,
      @Value("${app.schema.visa-sendpayout:classpath:schemas/visa-sendpayout-input.schema.json}") String schemaPath
  ) {
    this.objectMapper = objectMapper;
    this.schemaPath = schemaPath;
  }

  @Override
  public void process(Exchange exchange) {
    Object body = exchange.getMessage().getBody();
    try {
      JsonNode node = objectMapper.valueToTree(body);
      JsonSchema schema = getSchema();
      Set<ValidationMessage> errors = schema.validate(node);
      if (!errors.isEmpty()) {
        String first = errors.iterator().next().getMessage();
        throw new CartridgeException(ErrorCodes.code(ErrorCodes.VALIDATION_PATTERN), CartridgeException.ErrorType.FUNCTIONAL,
            "Schema validation failed: " + first, null, "VALIDATION");
      }
    } catch (CartridgeException e) {
      throw e;
    } catch (Exception e) {
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.GENERIC_TECHNICAL), CartridgeException.ErrorType.TECHNICAL,
          "Failed to validate JSON schema", e, null, "VALIDATION");
    }
  }

  private JsonSchema getSchema() {
    JsonSchema current = schema;
    if (current != null) {
      return current;
    }
    synchronized (this) {
      if (schema == null) {
        schema = loadSchema();
      }
      return schema;
    }
  }

  private JsonSchema loadSchema() {
    try (InputStream is = SchemaValidateProcessor.class.getClassLoader().getResourceAsStream(stripClasspath(schemaPath))) {
      if (is == null) {
        throw new IllegalStateException("Schema not found: " + schemaPath);
      }
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
      return factory.getSchema(is);
    } catch (Exception e) {
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.GENERIC_TECHNICAL), CartridgeException.ErrorType.TECHNICAL,
          "Failed to load JSON schema: " + schemaPath, e, null, "VALIDATION");
    }
  }

  private static String stripClasspath(String path) {
    return path.startsWith("classpath:") ? path.substring("classpath:".length()) : path;
  }
}
