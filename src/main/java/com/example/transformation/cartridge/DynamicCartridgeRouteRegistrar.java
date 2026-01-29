package com.example.transformation.cartridge;

import com.example.transformation.config.ConfigLoader;
import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs available cartridge routes after Camel has started.
 * Routes are loaded automatically via camel.springboot.routes-include-pattern in application.yaml
 */
@Component
public class DynamicCartridgeRouteRegistrar {

    private static final Logger log = LoggerFactory.getLogger(DynamicCartridgeRouteRegistrar.class);

    private final CamelContext camelContext;
    private final ConfigLoader configLoader;

    public DynamicCartridgeRouteRegistrar(CamelContext camelContext, ConfigLoader configLoader) {
        this.camelContext = camelContext;
        this.configLoader = configLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Active routes: {}", camelContext.getRoutes().size());
        
        // Log all route IDs
        for (var route : camelContext.getRoutes()) {
            log.info("  Route active: {} -> {}", route.getRouteId(), route.getEndpoint().getEndpointUri());
        }
        
        logAvailableCartridges();
    }

    private void logAvailableCartridges() {
        var schemaMaster = configLoader.getSchemaMasterConfig();
        var cartridgeMaster = configLoader.getCartridgeMasterConfig();

        log.info("Available cartridges:");
        schemaMaster.cartridges().forEach((id, schema) ->
                log.info("  {} -> provider: {}, format: {}", id, schema.provider(), schema.inputFormat()));

        log.info("Providers:");
        cartridgeMaster.providers().forEach((provider, config) ->
                log.info("  {} -> cartridges: {}", provider, config.cartridges()));
    }
}
