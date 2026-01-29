package com.example.transformation.cartridge;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonMappingEngine {
  private final MappingEngine mappingEngine;

  public JsonMappingEngine(MappingEngine mappingEngine) {
    this.mappingEngine = mappingEngine;
  }

  public MappingEngine.Result transform(Object input, MappingDefinition def) {
    Map<String, Object> out = mappingEngine.map(input, def);
    return MappingEngine.Result.json(out);
  }
}
