package com.example.transformation.web;

import com.example.transformation.config.CartridgeResolver;
import com.example.transformation.config.model.ResolvedCartridgeContext;
import com.example.transformation.processor.ExchangeKeys;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for CJSON to API transformations.
 * 
 * Endpoints:
 * - POST /api/transform/{cartridgeId} - Single request
 * - POST /api/transform/{cartridgeId}/bulk - Bulk request
 * 
 * Headers:
 * - X-Currency (optional): Currency code for currency-specific templates (USD, EUR, INR)
 * - X-Direction (optional): Flow direction (outbound/inbound), defaults to outbound
 */
@RestController
@RequestMapping("/api/transform")
public class TransformationController {

    private static final Logger log = LoggerFactory.getLogger(TransformationController.class);

    private final ProducerTemplate producerTemplate;
    private final CartridgeResolver cartridgeResolver;

    public TransformationController(ProducerTemplate producerTemplate, CartridgeResolver cartridgeResolver) {
        this.producerTemplate = producerTemplate;
        this.cartridgeResolver = cartridgeResolver;
    }

    /**
     * Single request transformation.
     * POST /api/transform/{cartridgeId}
     */
    @PostMapping(value = "/{cartridgeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> transform(
            @PathVariable String cartridgeId,
            @RequestHeader(value = "X-Currency", required = false) String currency,
            @RequestHeader(value = "X-Direction", required = false, defaultValue = "outbound") String direction,
            @RequestBody Map<String, Object> body) {

        ResolvedCartridgeContext context = cartridgeResolver.resolve(cartridgeId, currency, direction);
        log.info("Resolved context: endpoint={}, mappingPath={}", context.directEndpoint(), context.mappingPath());

        Exchange out = producerTemplate.request(context.directEndpoint(), e -> {
            e.getMessage().setBody(body);
            e.getMessage().setHeader(ExchangeKeys.CARTRIDGE_ID_HEADER, cartridgeId);
            e.getMessage().setHeader(ExchangeKeys.CURRENCY_HEADER, currency);
            e.getMessage().setHeader(ExchangeKeys.DIRECTION_HEADER, direction);
        });

        // Check for exceptions in the exchange
        if (out.getException() != null) {
            log.error("Exchange exception: ", out.getException());
            throw new RuntimeException("Transformation failed", out.getException());
        }

        log.info("Transformation complete, response body type: {}", 
                out.getMessage().getBody() != null ? out.getMessage().getBody().getClass().getSimpleName() : "null");

        return buildResponse(out);
    }

    /**
     * Bulk request transformation.
     * POST /api/transform/{cartridgeId}/bulk
     */
    @PostMapping(value = "/{cartridgeId}/bulk", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> transformBulk(
            @PathVariable String cartridgeId,
            @RequestHeader(value = "X-Currency", required = false) String currency,
            @RequestHeader(value = "X-Direction", required = false, defaultValue = "outbound") String direction,
            @RequestBody List<Map<String, Object>> body) {

        ResolvedCartridgeContext context = cartridgeResolver.resolve(cartridgeId, currency, direction);

        Exchange out = producerTemplate.request(context.directEndpoint(), e -> {
            e.getMessage().setBody(body);
            e.getMessage().setHeader(ExchangeKeys.CARTRIDGE_ID_HEADER, cartridgeId);
            e.getMessage().setHeader(ExchangeKeys.CURRENCY_HEADER, currency);
            e.getMessage().setHeader(ExchangeKeys.DIRECTION_HEADER, direction);
            e.getMessage().setHeader(ExchangeKeys.BULK_HEADER, true);
        });

        return buildResponse(out);
    }

    private ResponseEntity<?> buildResponse(Exchange out) {
        Object responseBody = out.getMessage().getBody();
        String contentType = out.getMessage().getHeader(Exchange.CONTENT_TYPE, String.class);
        MediaType mt = (contentType == null || contentType.isBlank()) 
                ? MediaType.APPLICATION_JSON 
                : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok().contentType(mt).body(responseBody);
    }
}
