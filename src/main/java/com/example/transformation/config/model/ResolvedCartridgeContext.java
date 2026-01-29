package com.example.transformation.config.model;

/**
 * Immutable resolved cartridge context after lookup chain completes.
 * Contains all information needed to process a transformation request.
 */
public record ResolvedCartridgeContext(
        String provider,
        String cartridgeId,
        String currency,
        String direction,
        String flowId,
        String inputFormat,
        String fromFormat,
        String toFormat,
        String mappingPath,
        String enrichPath,
        String routePath
) {
    /**
     * Returns the Camel direct endpoint URI for this context.
     */
    public String directEndpoint() {
        return (currency != null && !currency.isEmpty())
                ? "direct:" + cartridgeId + "-" + currency
                : "direct:" + cartridgeId;
    }

    /**
     * Creates a cache key for this context resolution.
     */
    public static String cacheKey(String cartridgeId, String currency, String direction) {
        return cartridgeId + "|" + (currency != null ? currency : "") + "|" + direction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String provider, cartridgeId, currency, direction, flowId;
        private String inputFormat, fromFormat, toFormat;
        private String mappingPath, enrichPath, routePath;

        public Builder provider(String v) { provider = v; return this; }
        public Builder cartridgeId(String v) { cartridgeId = v; return this; }
        public Builder currency(String v) { currency = v; return this; }
        public Builder direction(String v) { direction = v; return this; }
        public Builder flowId(String v) { flowId = v; return this; }
        public Builder inputFormat(String v) { inputFormat = v; return this; }
        public Builder fromFormat(String v) { fromFormat = v; return this; }
        public Builder toFormat(String v) { toFormat = v; return this; }
        public Builder mappingPath(String v) { mappingPath = v; return this; }
        public Builder enrichPath(String v) { enrichPath = v; return this; }
        public Builder routePath(String v) { routePath = v; return this; }

        public ResolvedCartridgeContext build() {
            return new ResolvedCartridgeContext(
                    provider, cartridgeId, currency, direction, flowId,
                    inputFormat, fromFormat, toFormat,
                    mappingPath, enrichPath, routePath);
        }
    }
}
