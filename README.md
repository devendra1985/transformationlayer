# Transformation Layer

A pluggable transformation service built with **Spring Boot 3.3.6** and **Apache Camel 4.8.2**.

## Architecture

The transformation layer uses a **cartridge-based** architecture where each cartridge defines a complete transformation pipeline:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        CJSON Input Message                               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  STAGE 1: VALIDATION                                                     │
│  • Required field checks                                                 │
│  • minLength / maxLength validation                                      │
│  • Pattern (regex) validation                                            │
│  • Numeric min / max validation                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  STAGE 2: ENRICHMENT / ENHANCEMENT                                       │
│  • Set static values (channel, sourceSystem, timestamp)                  │
│  • Copy fields from source                                               │
│  • Call Spring beans for dynamic enrichment                              │
│  • Conditional enrichment (when clause)                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  STAGE 3: TRANSFORMATION / MAPPING                                       │
│  • Map source fields to target fields                                    │
│  • Apply default values                                                  │
│  • Output as JSON or XML                                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      REST API Output                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
src/main/
├── java/com/example/transformation/
│   ├── cartridge/           # Core cartridge framework
│   │   ├── CartridgeException.java
│   │   ├── CartridgeYamlRouteRegistrar.java
│   │   ├── JsonPathMini.java
│   │   ├── MappingDefinition.java
│   │   ├── MappingEngine.java
│   │   └── MappingLoader.java
│   ├── enrich/              # Enrichment framework
│   │   ├── EnrichmentConfig.java
│   │   ├── EnrichmentEngine.java
│   │   ├── EnrichmentLoader.java
│   │   ├── ExampleEnrichmentFunctions.java
│   │   └── PaymentEnrichmentFunctions.java
│   ├── processor/           # Camel processors
│   │   ├── EnrichProcessor.java
│   │   ├── ExchangeKeys.java
│   │   ├── TransformProcessor.java
│   │   └── ValidateProcessor.java
│   ├── web/                 # REST API
│   │   ├── ApiExceptionHandler.java
│   │   └── TransformationController.java
│   └── TransformationServiceApplication.java
└── resources/
    ├── application.yaml
    └── cartridges/
        ├── cjson-to-rest/
        │   ├── cjson-to-rest-route.yaml
        │   ├── cjson-to-rest-mapping.yaml
        │   └── cjson-to-rest-enrich.yaml
        └── cjson-to-payment-api/
            ├── cjson-to-payment-api-route.yaml
            ├── cjson-to-payment-api-mapping.yaml
            └── cjson-to-payment-api-enrich.yaml
```

## Quick Start

### Build

```bash
mvn clean package
```

### Run

```bash
mvn spring-boot:run
```

### Test

```bash
# cjson-to-rest cartridge
curl -X POST http://localhost:8080/api/transform/cjson-to-rest \
  -H "Content-Type: application/json" \
  -d '{
    "msgId": "MSG-20260115-0001",
    "currency": "USD",
    "amount": 100.50,
    "bic": "DEUTDEFF"
  }'

# cjson-to-payment-api cartridge
curl -X POST http://localhost:8080/api/transform/cjson-to-payment-api \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN-2026-01-15-ABC123",
    "paymentType": "WIRE",
    "payer": {
      "accountNumber": "1234567890123456",
      "name": "John Doe"
    },
    "payee": {
      "accountNumber": "9876543210987654",
      "name": "Acme Corp",
      "bankCode": "DEUTDEFF"
    },
    "amount": 15000.00,
    "currency": "USD"
  }'
```

## Creating a New Cartridge

To create a new cartridge, add a folder under `src/main/resources/cartridges/`:

```
cartridges/
└── my-new-cartridge/
    ├── my-new-cartridge-route.yaml      # Camel route definition
    ├── my-new-cartridge-mapping.yaml    # Validation + mapping rules
    └── my-new-cartridge-enrich.yaml     # Enrichment rules (optional)
```

The system **auto-discovers** cartridges at startup.

## Configuration

See `application.yaml` for configuration options:

- `app.cartridges.base-path` - Base path for cartridge files
- `app.downstream.base-url` - Default downstream API URL
- `app.payment-api.base-url` - Payment API URL

