package com.example.transformation.config;

import com.example.transformation.cartridge.MappingLoader;
import com.example.transformation.enrich.EnrichmentLoader;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pre-warms caches at startup to avoid cold-start latency on first requests.
 * 
 * This ensures:
 * - All cartridge contexts are resolved and cached
 * - All mapping definitions are loaded and cached
 * - All enrichment configs are loaded and cached
 * - Template existence checks are cached
 */
@Component
public class CacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmer.class);
    private static final String[] COMMON_CURRENCIES = {"USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD"};
    private static final String[] DIRECTIONS = {"outbound", "inbound"};

    private final ConfigLoader configLoader;
    private final CartridgeResolver cartridgeResolver;
    private final MappingLoader mappingLoader;
    private final EnrichmentLoader enrichmentLoader;
    private final boolean warmCaches;

    public CacheWarmer(
            ConfigLoader configLoader,
            CartridgeResolver cartridgeResolver,
            MappingLoader mappingLoader,
            EnrichmentLoader enrichmentLoader,
            @Value("${app.cache.warm-on-startup:true}") boolean warmCaches) {
        this.configLoader = configLoader;
        this.cartridgeResolver = cartridgeResolver;
        this.mappingLoader = mappingLoader;
        this.enrichmentLoader = enrichmentLoader;
        this.warmCaches = warmCaches;
    }

    @PostConstruct
    public void warmCaches() {
        if (!warmCaches) {
            log.info("Cache warming disabled");
            return;
        }

        log.info("Warming caches...");
        long start = System.currentTimeMillis();

        int contextsWarmed = 0;
        int mappingsWarmed = 0;
        int enrichmentsWarmed = 0;

        // Iterate all known cartridges
        var cartridges = configLoader.getSchemaMasterConfig().cartridges();
        
        for (String cartridgeId : cartridges.keySet()) {
            for (String direction : DIRECTIONS) {
                // Warm base cartridge (no currency)
                try {
                    var context = cartridgeResolver.resolve(cartridgeId, null, direction);
                    contextsWarmed++;
                    
                    // Pre-load mapping and enrichment
                    mappingLoader.load(context.mappingPath());
                    mappingsWarmed++;
                    
                    enrichmentLoader.loadOptional(context.enrichPath());
                    enrichmentsWarmed++;
                } catch (Exception e) {
                    log.debug("Skipping base {} {}: {}", cartridgeId, direction, e.getMessage());
                }

                // Warm common currencies
                for (String currency : COMMON_CURRENCIES) {
                    try {
                        if (cartridgeResolver.currencyTemplateExists(cartridgeId, currency)) {
                            var context = cartridgeResolver.resolve(cartridgeId, currency, direction);
                            contextsWarmed++;
                            
                            mappingLoader.load(context.mappingPath());
                            mappingsWarmed++;
                            
                            enrichmentLoader.loadOptional(context.enrichPath());
                            enrichmentsWarmed++;
                        }
                    } catch (Exception e) {
                        // Currency template doesn't exist - that's fine
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Cache warming completed in {}ms: {} contexts, {} mappings, {} enrichments",
                elapsed, contextsWarmed, mappingsWarmed, enrichmentsWarmed);
    }
}
