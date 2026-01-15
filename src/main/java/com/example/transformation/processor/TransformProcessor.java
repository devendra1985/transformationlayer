package com.example.transformation.processor;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.MappingDefinition;
import com.example.transformation.cartridge.MappingEngine;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("transform")
public class TransformProcessor implements Processor {
  private final MappingEngine engine;

  public TransformProcessor(MappingEngine engine) {
    this.engine = engine;
  }

  @Override
  public void process(Exchange exchange) {
    MappingDefinition def = exchange.getProperty(ExchangeKeys.MAPPING_DEF_PROP, MappingDefinition.class);
    if (def == null) {
      throw new CartridgeException("Missing mapping definition (did you run validate processor first?)");
    }

    Object input = exchange.getMessage().getBody();
    MappingEngine.Result result = engine.transform(input, def);
    exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, result.contentType());
    exchange.getMessage().setBody(result.body());
  }
}

