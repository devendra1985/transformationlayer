package com.example.transformation.processor;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.MappingDefinition;
import com.example.transformation.cartridge.MappingLoader;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("validate")
public class ValidateProcessor implements Processor {
  private final MappingLoader loader;
  private final String basePath;

  public ValidateProcessor(
      MappingLoader loader,
      @Value("${app.cartridges.base-path:classpath:cartridges}") String basePath
  ) {
    this.loader = loader;
    this.basePath = basePath;
  }

  @Override
  public void process(Exchange exchange) {
    Object body = exchange.getMessage().getBody();
    if (!(body instanceof Map || body instanceof List)) {
      throw new CartridgeException("Expected JSON body parsed to Map/List but got: " + (body == null ? "null" : body.getClass()));
    }

    String cartridgeId = exchange.getMessage().getHeader(ExchangeKeys.CARTRIDGE_ID_HEADER, String.class);
    if (cartridgeId == null || cartridgeId.isBlank()) {
      throw new CartridgeException("Missing cartridge id header: " + ExchangeKeys.CARTRIDGE_ID_HEADER);
    }

    String mappingPath = basePath + "/" + cartridgeId + "/" + cartridgeId + "-mapping.yaml";
    MappingDefinition def = loader.load(mappingPath);
    exchange.setProperty(ExchangeKeys.MAPPING_DEF_PROP, def);
  }
}

