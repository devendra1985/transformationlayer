package com.example.transformation.cartridge;

import com.example.transformation.config.ConfigLoader;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * Programmatically creates Camel routes for each cartridge and currency combination.
 * This ensures routes are properly registered before Camel context starts.
 */
@Component
public class CartridgeRouteBuilder extends RouteBuilder {

    private final ConfigLoader configLoader;

    public CartridgeRouteBuilder(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @Override
    public void configure() throws Exception {
        // Get all cartridges from schema-master
        var cartridges = configLoader.getSchemaMasterConfig().cartridges();
        
        // Common currencies to create routes for
        String[] currencies = {"USD", "EUR", "INR"};
        
        for (String cartridgeId : cartridges.keySet()) {
            // Create base route (no currency)
            createRoute(cartridgeId, null);
            
            // Create currency-specific routes
            for (String currency : currencies) {
                createRoute(cartridgeId, currency);
            }
        }
    }

    private void createRoute(String cartridgeId, String currency) {
        String routeId = currency != null 
                ? cartridgeId + "-" + currency + "-route"
                : cartridgeId + "-route";
        
        String endpoint = currency != null 
                ? "direct:" + cartridgeId + "-" + currency
                : "direct:" + cartridgeId;

        from(endpoint)
                .routeId(routeId)
                .bean("persistRaw", "process")
                .bean("validate", "process")
                .bean("enrich", "process")
                .bean("transform", "process")
                .bean("persistTransformed", "process")
                .setHeader("Content-Type", constant("application/json"));
    }
}
