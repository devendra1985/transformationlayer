package com.example.transformation.enrich;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Payment-specific enrichment functions.
 * 
 * These methods are called from the cartridge enrichment YAML via the 'call' directive.
 * 
 * Convention:
 *   - Method signature: Object methodName(Map<String, Object> body)
 *   - Returns a Map that will be merged into the body, or a single value if 'target' is specified
 */
@Component("paymentEnrichmentFunctions")
public class PaymentEnrichmentFunctions {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentEnrichmentFunctions.class);
    
    // Risk thresholds
    private static final double HIGH_VALUE_THRESHOLD = 10000.0;
    private static final double VERY_HIGH_VALUE_THRESHOLD = 50000.0;
    
    /**
     * Calculate risk score and level based on payment details.
     * 
     * Risk factors considered:
     * - Transaction amount (higher = more risk)
     * - Payment type (WIRE/INSTANT = higher risk)
     * - Cross-border indicators
     * 
     * @param body The incoming payment message
     * @return Map containing riskScore (0-100) and riskLevel (LOW/MEDIUM/HIGH/CRITICAL)
     */
    public Map<String, Object> calculateRiskScore(Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        int riskScore = 0;
        
        // Factor 1: Amount-based risk
        Double amount = extractAmount(body);
        if (amount != null) {
            if (amount >= VERY_HIGH_VALUE_THRESHOLD) {
                riskScore += 40;
            } else if (amount >= HIGH_VALUE_THRESHOLD) {
                riskScore += 25;
            } else if (amount >= 5000) {
                riskScore += 10;
            }
        }
        
        // Factor 2: Payment type risk
        String paymentType = extractString(body, "paymentType");
        if ("WIRE".equalsIgnoreCase(paymentType)) {
            riskScore += 20;
        } else if ("INSTANT".equalsIgnoreCase(paymentType)) {
            riskScore += 15;
        } else if ("ACH".equalsIgnoreCase(paymentType)) {
            riskScore += 5;
        }
        
        // Factor 3: Cross-border indicator (if payee bank code is international)
        Map<?, ?> payee = extractMap(body, "payee");
        if (payee != null) {
            String bankCode = (String) payee.get("bankCode");
            if (bankCode != null && bankCode.length() == 11) {
                // 11-char BIC suggests international
                riskScore += 15;
            }
        }
        
        // Factor 4: New payee (if flagged)
        Boolean newPayee = extractBoolean(body, "newPayee");
        if (Boolean.TRUE.equals(newPayee)) {
            riskScore += 10;
        }
        
        // Cap at 100
        riskScore = Math.min(riskScore, 100);
        
        // Determine risk level
        String riskLevel;
        if (riskScore >= 70) {
            riskLevel = "CRITICAL";
        } else if (riskScore >= 50) {
            riskLevel = "HIGH";
        } else if (riskScore >= 25) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }
        
        result.put("riskScore", riskScore);
        result.put("riskLevel", riskLevel);
        
        log.debug("Calculated risk score: {} ({})", riskScore, riskLevel);
        
        return result;
    }
    
    /**
     * Enrich payer details from customer database lookup.
     * 
     * In a real implementation, this would call a customer service or database.
     * For demo purposes, this adds sample enrichment data.
     * 
     * @param body The incoming payment message (must contain payer.customerId)
     * @return Map containing enriched payer details
     */
    public Map<String, Object> enrichPayerDetails(Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        Map<?, ?> payer = extractMap(body, "payer");
        if (payer == null) {
            return result;
        }
        
        String customerId = (String) payer.get("customerId");
        if (customerId == null || customerId.isBlank()) {
            return result;
        }
        
        // In production: call customer service API
        // CustomerDetails details = customerService.getCustomer(customerId);
        
        // Demo: add sample enrichment
        Map<String, Object> payerEnrichment = new LinkedHashMap<>();
        payerEnrichment.put("customerTier", "PREMIUM");
        payerEnrichment.put("kycVerified", true);
        payerEnrichment.put("accountAge", "5 years");
        
        result.put("payerDetails", payerEnrichment);
        
        log.debug("Enriched payer details for customerId: {}", customerId);
        
        return result;
    }
    
    /**
     * Validate and normalize IBAN format.
     * 
     * @param body The incoming payment message
     * @return Map containing normalized IBAN if valid
     */
    public Map<String, Object> normalizeIban(Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        Map<?, ?> payee = extractMap(body, "payee");
        if (payee == null) {
            return result;
        }
        
        String accountNumber = (String) payee.get("accountNumber");
        if (accountNumber != null) {
            // Remove spaces and convert to uppercase
            String normalized = accountNumber.replaceAll("\\s+", "").toUpperCase();
            result.put("payee.normalizedAccount", normalized);
        }
        
        return result;
    }
    
    // =========================================================================
    // Helper methods
    // =========================================================================
    
    private Double extractAmount(Map<String, Object> body) {
        Object amount = body.get("amount");
        if (amount instanceof Number n) {
            return n.doubleValue();
        }
        if (amount instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String extractString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value instanceof String s ? s : null;
    }
    
    private Boolean extractBoolean(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return "true".equalsIgnoreCase(s);
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return null;
    }
}

