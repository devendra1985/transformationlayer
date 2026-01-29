package com.example.transformation.processor;

import com.example.transformation.persistence.PayloadPersistenceService;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("persistRaw")
public class RawPersistProcessor implements Processor {
  private final ObjectProvider<PayloadPersistenceService> persistenceService;
  private final boolean persistenceEnabled;

  public RawPersistProcessor(
      ObjectProvider<PayloadPersistenceService> persistenceService,
      @Value("${app.persistence.enabled:false}") boolean persistenceEnabled
  ) {
    this.persistenceService = persistenceService;
    this.persistenceEnabled = persistenceEnabled;
  }

  @Override
  public void process(Exchange exchange) {
    Object body = exchange.getMessage().getBody();
    String requestId = resolveRequestId(body);
    exchange.getMessage().setHeader("X-Request-Id", requestId);
    if (persistenceEnabled) {
      PayloadPersistenceService svc = persistenceService.getIfAvailable();
      if (svc != null) {
        svc.storeRaw(requestId, body, "RECEIVED");
      }
    }
  }

  @SuppressWarnings("unchecked")
  private String resolveRequestId(Object body) {
    if (body instanceof Map<?, ?> m) {
      Object header = m.get("header");
      if (header instanceof Map<?, ?> h) {
        Object corrId = h.get("correlationId");
        if (corrId != null && !String.valueOf(corrId).isBlank()) {
          return String.valueOf(corrId);
        }
      }
      Object paymentId = m.get("paymentId");
      if (paymentId != null && !String.valueOf(paymentId).isBlank()) {
        return String.valueOf(paymentId);
      }
      Object paymentData = m.get("paymentData");
      if (paymentData instanceof Map<?, ?> pd) {
        Object txInf = pd.get("txInf");
        if (txInf instanceof java.util.List<?> list && !list.isEmpty()) {
          Object first = list.get(0);
          if (first instanceof Map<?, ?> firstMap) {
            Object txPaymentId = firstMap.get("paymentId");
            if (txPaymentId != null && !String.valueOf(txPaymentId).isBlank()) {
              return String.valueOf(txPaymentId);
            }
          }
        }
      }
    }
    return UUID.randomUUID().toString();
  }
}
