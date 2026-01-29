package com.example.transformation.processor;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.ErrorCodes;
import com.example.transformation.cartridge.JsonMappingEngine;
import com.example.transformation.cartridge.MappingDefinition;
import com.example.transformation.cartridge.MappingEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Transform processor for CJSON to API transformations.
 * Supports both single and bulk request processing.
 */
@Component("transform")
public class TransformProcessor implements Processor {

    private final JsonMappingEngine jsonEngine;
    private final int bulkParallelism;
    private final ForkJoinPool bulkPool;

    public TransformProcessor(
            JsonMappingEngine jsonEngine,
            @Value("${app.bulk.parallelism:0}") int bulkParallelism) {
        this.jsonEngine = jsonEngine;
        this.bulkParallelism = bulkParallelism;
        this.bulkPool = (bulkParallelism > 1) ? new ForkJoinPool(bulkParallelism) : null;
    }

    @Override
    public void process(Exchange exchange) {
        MappingDefinition def = exchange.getProperty(ExchangeKeys.MAPPING_DEF_PROP, MappingDefinition.class);
        if (def == null) {
            throw new CartridgeException(
                    ErrorCodes.code(ErrorCodes.MAPPING_DEFINITION_MISSING),
                    CartridgeException.ErrorType.FUNCTIONAL,
                    "Missing mapping definition (did you run validate processor first?)",
                    null, "TRANSFORM");
        }

        Object input = exchange.getMessage().getBody();

        // Bulk processing
        if (input instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<BulkRecord> records = (List<BulkRecord>) list;
            processBulk(records, def);

            List<Object> results = new ArrayList<>(records.size());
            for (BulkRecord record : records) {
                results.add(record.toResponse());
            }
            exchange.getMessage().setHeader(ExchangeKeys.BULK_HEADER, true);
            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
            exchange.getMessage().setBody(results);
            return;
        }

        // Single request processing
        MappingEngine.Result result = jsonEngine.transform(input, def);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, result.contentType());
        exchange.getMessage().setBody(result.body());
    }

    private void processBulk(List<BulkRecord> records, MappingDefinition def) {
        runInParallel(records, record -> {
            if (!record.hasError()) {
                try {
                    MappingEngine.Result result = jsonEngine.transform(record.getInput(), def);
                    record.setOutput(result.body());
                    record.setContentType(result.contentType());
                } catch (CartridgeException e) {
                    record.setError(BulkError.from(e));
                }
            }
        });
    }

    private void runInParallel(List<BulkRecord> records, java.util.function.Consumer<BulkRecord> work) {
        if (bulkParallelism <= 1) {
            records.forEach(work);
            return;
        }
        try {
            bulkPool.submit(() -> records.parallelStream().forEach(work)).get();
        } catch (Exception e) {
            throw new CartridgeException(
                    ErrorCodes.code(ErrorCodes.GENERIC_TECHNICAL),
                    CartridgeException.ErrorType.TECHNICAL,
                    "Bulk parallel transform failed", e, null, "TRANSFORM");
        }
    }
}
