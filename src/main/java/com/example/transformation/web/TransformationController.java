package com.example.transformation.web;

import com.example.transformation.processor.ExchangeKeys;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/transform")
public class TransformationController {
  private final ProducerTemplate producerTemplate;

  public TransformationController(ProducerTemplate producerTemplate) {
    this.producerTemplate = producerTemplate;
  }

  /**
   * Plug-and-play entrypoint:
   * POST /api/transform/{cartridgeId}
   *
   * Rule: each cartridge must define a Camel route starting with: direct:{cartridgeId}
   */
  @PostMapping(value = "/{cartridgeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> transform(@PathVariable String cartridgeId, @RequestBody Map<String, Object> body) {
    Exchange out = producerTemplate.request("direct:" + cartridgeId, e -> {
      e.getMessage().setBody(body);
      e.getMessage().setHeader(ExchangeKeys.CARTRIDGE_ID_HEADER, cartridgeId);
    });

    Object responseBody = out.getMessage().getBody();
    String contentType = out.getMessage().getHeader(Exchange.CONTENT_TYPE, String.class);
    MediaType mt = (contentType == null || contentType.isBlank()) ? MediaType.APPLICATION_JSON : MediaType.parseMediaType(contentType);
    return ResponseEntity.ok().contentType(mt).body(responseBody);
  }
}

