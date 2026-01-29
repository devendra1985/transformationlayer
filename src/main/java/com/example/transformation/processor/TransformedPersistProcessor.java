package com.example.transformation.processor;

import com.example.transformation.persistence.PayloadPersistenceService;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("persistTransformed")
public class TransformedPersistProcessor implements Processor {
  private final ObjectProvider<PayloadPersistenceService> persistenceService;
  private final boolean persistenceEnabled;

  public TransformedPersistProcessor(
      ObjectProvider<PayloadPersistenceService> persistenceService,
      @Value("${app.persistence.enabled:false}") boolean persistenceEnabled
  ) {
    this.persistenceService = persistenceService;
    this.persistenceEnabled = persistenceEnabled;
  }

  @Override
  public void process(Exchange exchange) {
    Object body = exchange.getMessage().getBody();
    String requestId = exchange.getMessage().getHeader("X-Request-Id", String.class);
    if (requestId == null || requestId.isBlank()) {
      requestId = "UNKNOWN";
    }
    if (persistenceEnabled) {
      PayloadPersistenceService svc = persistenceService.getIfAvailable();
      if (svc != null) {
        svc.storeTransformed(requestId, body, "TRANSFORMED");
      }
    }
  }
}
