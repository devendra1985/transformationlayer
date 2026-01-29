package com.example.transformation.processor;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.ErrorCodes;
import com.example.transformation.cartridge.MappingDefinition;
import com.example.transformation.cartridge.MappingLoader;
import com.example.transformation.config.CartridgeResolver;
import com.example.transformation.config.model.ResolvedCartridgeContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("validate")
public class ValidateProcessor implements Processor {
  private final MappingLoader loader;
  private final CartridgeResolver cartridgeResolver;

  public ValidateProcessor(
      MappingLoader loader,
      CartridgeResolver cartridgeResolver
  ) {
    this.loader = loader;
    this.cartridgeResolver = cartridgeResolver;
  }

  @Override
  public void process(Exchange exchange) {
    Object body = exchange.getMessage().getBody();
    if (body instanceof List<?> list) {
      List<BulkRecord> records = new ArrayList<>(list.size());
      for (int i = 0; i < list.size(); i++) {
        Object item = list.get(i);
        if (item instanceof Map<?, ?> m) {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) m;
          records.add(new BulkRecord(i, map));
        } else {
          BulkRecord record = new BulkRecord(i, null);
          record.setError(BulkError.functional(
              "Expected JSON object at index " + i + " but got: " + (item == null ? "null" : item.getClass()),
              null,
              "VALIDATION"
          ));
          records.add(record);
        }
      }
      exchange.getMessage().setBody(records);
    } else if (body instanceof Map<?, ?> m) {
      List<BulkRecord> records = maybeExtractBulkRecords(m);
      if (records != null) {
        exchange.getMessage().setBody(records);
      }
    } else {
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.REQUEST_BODY_TYPE), CartridgeException.ErrorType.FUNCTIONAL,
          "Expected JSON body parsed to Map/List but got: " + (body == null ? "null" : body.getClass()), null,
          "VALIDATION");
    }

    String cartridgeId = exchange.getMessage().getHeader(ExchangeKeys.CARTRIDGE_ID_HEADER, String.class);
    if (cartridgeId == null || cartridgeId.isBlank()) {
      throw new CartridgeException(ErrorCodes.code(ErrorCodes.REQUEST_CARTRIDGE_ID_MISSING), CartridgeException.ErrorType.FUNCTIONAL,
          "Missing cartridge id header: " + ExchangeKeys.CARTRIDGE_ID_HEADER, null, "VALIDATION");
    }

    String currency = exchange.getMessage().getHeader(ExchangeKeys.CURRENCY_HEADER, String.class);
    String direction = exchange.getMessage().getHeader(ExchangeKeys.DIRECTION_HEADER, "outbound", String.class);

    // Resolve cartridge context using the lookup chain
    ResolvedCartridgeContext context = cartridgeResolver.resolve(cartridgeId, currency, direction);
    exchange.setProperty(ExchangeKeys.RESOLVED_CONTEXT_PROP, context);

    // Load mapping definition from resolved path
    MappingDefinition def = loader.load(context.mappingPath());
    exchange.setProperty(ExchangeKeys.MAPPING_DEF_PROP, def);
  }

  @SuppressWarnings("unchecked")
  private List<BulkRecord> maybeExtractBulkRecords(Map<?, ?> root) {
    Object paymentDataObj = root.get("paymentData");
    if (!(paymentDataObj instanceof Map<?, ?> paymentData)) {
      return null;
    }
    Object txInfObj = paymentData.get("txInf");
    if (!(txInfObj instanceof List<?> txInfList)) {
      return null;
    }

    Map<String, Object> header = (root.get("header") instanceof Map<?, ?> h) ? (Map<String, Object>) h : null;
    Map<String, Object> grpHdr = firstMapFromList(paymentData.get("grpHdr"));
    Map<String, Object> bulk = firstMapFromList(paymentData.get("bulk"));

    List<BulkRecord> records = new ArrayList<>(txInfList.size());
    for (int i = 0; i < txInfList.size(); i++) {
      Object item = txInfList.get(i);
      if (item instanceof Map<?, ?> txInf) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.putAll((Map<String, Object>) txInf);
        if (header != null) merged.put("header", header);
        if (grpHdr != null) merged.put("grpHdr", grpHdr);
        if (bulk != null) merged.put("bulk", bulk);
        records.add(new BulkRecord(i, merged));
      } else {
        BulkRecord record = new BulkRecord(i, null);
        record.setError(BulkError.functional(
            "Expected txInf JSON object at index " + i + " but got: " + (item == null ? "null" : item.getClass()),
            null,
            "VALIDATION"
        ));
        records.add(record);
      }
    }
    return records;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> firstMapFromList(Object listObj) {
    if (!(listObj instanceof List<?> list) || list.isEmpty()) {
      return null;
    }
    Object first = list.get(0);
    return (first instanceof Map<?, ?> m) ? (Map<String, Object>) m : null;
  }
}

