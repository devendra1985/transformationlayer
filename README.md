## Transformation Layer

A high-performance CJSON to API transformation service built with **Spring Boot 3.3.6** and **Apache Camel 4.8.2**.

## Features

- **Multi-level Configuration**: Provider → CartridgeId → Currency hierarchy
- **Currency-specific Templates**: USD, EUR, INR specific transformations
- **Startup Cache Warming**: Zero cold-start latency
- **Bulk Processing**: Parallel processing support for batch requests
- **Pluggable Architecture**: Add new cartridges without code changes

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        CJSON Input Message                               │
│                    + Headers: X-Currency, X-Direction                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  CONFIG RESOLUTION                                                       │
│  1. schema-master.yaml      → Lookup cartridgeId metadata               │
│  2. schema-flow-mapping.yaml → Get flowId for direction                 │
│  3. transformation-flow-master.yaml → Get flow definition               │
│  4. Resolve template path   → Currency-specific or fallback             │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  STAGE 1: VALIDATION                                                     │
│  • Required field checks                                                 │
│  • minLength / maxLength validation                                      │
│  • Pattern (regex) validation                                            │
│  • Numeric min / max validation                                          │
│  • Conditional validation (whenPath/whenEquals)                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  STAGE 2: ENRICHMENT                                                     │
│  • Set static values                                                     │
│  • Copy fields from source                                               │
│  • Call Spring beans for dynamic enrichment                              │
│  • Conditional enrichment (when clause)                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  STAGE 3: TRANSFORMATION                                                 │
│  • Map source fields to target fields                                    │
│  • Apply default values                                                  │
│  • Output as JSON                                                        │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      API Output (JSON)                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
src/main/
├── java/com/example/transformation/
│   ├── cartridge/              # Core transformation engine
│   │   ├── CartridgeException.java
│   │   ├── DynamicCartridgeRouteRegistrar.java
│   │   ├── ErrorCodes.java
│   │   ├── JsonMappingEngine.java
│   │   ├── JsonPathMini.java
│   │   ├── MappingDefinition.java
│   │   ├── MappingEngine.java
│   │   └── MappingLoader.java
│   ├── config/                 # Configuration loading & resolution
│   │   ├── CacheWarmer.java
│   │   ├── CartridgeResolver.java
│   │   ├── ConfigLoader.java
│   │   └── model/
│   │       ├── CartridgeMasterConfig.java
│   │       ├── ResolvedCartridgeContext.java
│   │       ├── SchemaFlowMappingConfig.java
│   │       ├── SchemaMasterConfig.java
│   │       └── TransformationFlowMasterConfig.java
│   ├── enrich/                 # Enrichment framework
│   │   ├── EnrichmentConfig.java
│   │   ├── EnrichmentEngine.java
│   │   ├── EnrichmentLoader.java
│   │   └── PaymentEnrichmentFunctions.java
│   ├── processor/              # Camel processors
│   │   ├── BulkError.java
│   │   ├── BulkRecord.java
│   │   ├── EnrichProcessor.java
│   │   ├── ExchangeKeys.java
│   │   ├── TransformProcessor.java
│   │   └── ValidateProcessor.java
│   ├── web/                    # REST API
│   │   ├── ApiExceptionHandler.java
│   │   └── TransformationController.java
│   └── TransformationServiceApplication.java
└── resources/
    ├── application.yaml
    ├── error-codes.properties
    ├── config/                 # Master configuration files
    │   ├── cartridge-master.yaml
    │   ├── schema-master.yaml
    │   ├── schema-flow-mapping.yaml
    │   └── transformation-flow-master.yaml
    └── cartridges/             # Cartridge templates
        └── VISA/
            └── VISABA/
                ├── mapping.yaml        # Base mapping (fallback)
                ├── enrich.yaml         # Base enrichment
                ├── route.yaml          # Base route
                ├── USD/                # USD-specific templates
                │   ├── mapping.yaml
                │   ├── enrich.yaml
                │   └── route.yaml
                ├── EUR/                # EUR-specific templates
                │   ├── mapping.yaml
                │   ├── enrich.yaml
                │   └── route.yaml
                └── INR/                # INR-specific templates
                    ├── mapping.yaml
                    ├── enrich.yaml
                    └── route.yaml
```

## Configuration Files

### 1. cartridge-master.yaml
Maps providers to their cartridge IDs.

```yaml
providers:
  VISA:
    name: "Visa Direct"
    cartridges:
      - VISABA
      - VISAWA
```

### 2. schema-master.yaml
Cartridge metadata keyed by cartridgeId.

```yaml
cartridges:
  VISABA:
    description: "Visa Bank Account Payout"
    provider: VISA
    inputFormat: CJSON
```

### 3. schema-flow-mapping.yaml
Maps cartridgeId to flow directions.

```yaml
cartridgeFlows:
  VISABA:
    outbound:
      flowId: CJSON_TO_API
    inbound:
      flowId: API_TO_CJSON
```

### 4. transformation-flow-master.yaml
Defines transformation flow types.

```yaml
flows:
  CJSON_TO_API:
    from: CJSON
    to: API
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

Or run in IntelliJ IDEA:
- Open `TransformationServiceApplication.java`
- Click the green "Run" button

### Test

#### Single Request (USD)

```bash
curl -X POST http://localhost:8080/api/transform/VISABA \
  -H "Content-Type: application/json" \
  -H "X-Currency: USD" \
  -d '{
    "paymentId": "PAY-2026-01-28-001",
    "cdtrNm": "John Doe",
    "cdtrAcctIban": "US1234567890123456",
    "cdtrPstlAdrCtry": "USA",
    "dbtrNm": "Acme Corp",
    "dbtrAcctIban": "US9876543210987654",
    "crPymtAmt": 1500.00,
    "crPymtAmtCcy": "USD"
  }'
```

#### Single Request (EUR)

```bash
curl -X POST http://localhost:8080/api/transform/VISABA \
  -H "Content-Type: application/json" \
  -H "X-Currency: EUR" \
  -d '{
    "paymentId": "PAY-2026-01-28-002",
    "cdtrNm": "Hans Mueller",
    "cdtrAcctIban": "DE89370400440532013000",
    "cdtrAgtBic": "DEUTDEFF",
    "cdtrPstlAdrCtry": "DEU",
    "dbtrNm": "Euro Corp",
    "dbtrAcctIban": "DE89370400440532013001",
    "crPymtAmt": 2500.00,
    "crPymtAmtCcy": "EUR"
  }'
```

#### Bulk Request

```bash
curl -X POST http://localhost:8080/api/transform/VISABA/bulk \
  -H "Content-Type: application/json" \
  -H "X-Currency: USD" \
  -d '[
    {
      "paymentId": "PAY-001",
      "cdtrNm": "John Doe",
      "cdtrAcctIban": "US1234567890",
      "cdtrPstlAdrCtry": "USA",
      "dbtrNm": "Sender 1",
      "dbtrAcctIban": "US0987654321",
      "crPymtAmt": 100.00,
      "crPymtAmtCcy": "USD"
    },
    {
      "paymentId": "PAY-002",
      "cdtrNm": "Jane Smith",
      "cdtrAcctIban": "US2345678901",
      "cdtrPstlAdrCtry": "USA",
      "dbtrNm": "Sender 2",
      "dbtrAcctIban": "US1098765432",
      "crPymtAmt": 200.00,
      "crPymtAmtCcy": "USD"
    }
  ]'
```

## API Reference

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transform/{cartridgeId}` | Single request transformation |
| POST | `/api/transform/{cartridgeId}/bulk` | Bulk request transformation |

### Headers

| Header | Required | Default | Description |
|--------|----------|---------|-------------|
| `Content-Type` | Yes | - | Must be `application/json` |
| `X-Currency` | No | - | Currency code (USD, EUR, INR) for currency-specific templates |
| `X-Direction` | No | `outbound` | Flow direction (`outbound` or `inbound`) |

### Response

Success response returns the transformed JSON with `Content-Type: application/json`.

## Error Handling

All errors return a structured JSON response:

```json
{
  "code": "FUNC-VALIDATION-REQUIRED",
  "type": "FUNCTIONAL",
  "message": "Validation failed: required field missing at paymentId",
  "field": "paymentId",
  "step": "VALIDATION"
}
```

### Error Types

- `FUNCTIONAL` - Business/validation errors (4xx)
- `TECHNICAL` - System/infrastructure errors (5xx)

### Steps

- `VALIDATION` - Request validation
- `ENRICHMENT` - Enrichment processing
- `TRANSFORM` - Mapping/transformation
- `RESOLVE` - Cartridge resolution
- `CONFIG` - Configuration loading

## Adding a New Cartridge

### 1. Add to Configuration Files

**schema-master.yaml:**
```yaml
cartridges:
  NEWCARTRIDGE:
    description: "New Cartridge Description"
    provider: PROVIDER_NAME
    inputFormat: CJSON
```

**cartridge-master.yaml:**
```yaml
providers:
  PROVIDER_NAME:
    name: "Provider Display Name"
    cartridges:
      - NEWCARTRIDGE
```

**schema-flow-mapping.yaml:**
```yaml
cartridgeFlows:
  NEWCARTRIDGE:
    outbound:
      flowId: CJSON_TO_API
```

### 2. Create Template Files

```
cartridges/PROVIDER_NAME/NEWCARTRIDGE/
├── mapping.yaml    # Validation + mapping rules
├── enrich.yaml     # Enrichment rules
└── route.yaml      # Camel route definition
```

### 3. (Optional) Add Currency-Specific Templates

```
cartridges/PROVIDER_NAME/NEWCARTRIDGE/USD/
├── mapping.yaml
├── enrich.yaml
└── route.yaml
```

## Performance Optimizations

- **Startup Cache Warming**: All configurations pre-loaded at startup
- **Interned Cache Keys**: Reduced GC pressure for frequent lookups
- **Jackson YAML**: Faster parsing than SnakeYAML
- **ConcurrentHashMap Caching**: Thread-safe O(1) lookups
- **Pre-built Base Paths**: No string concatenation in hot paths

## Configuration

See `application.yaml` for all options:

```yaml
app:
  config:
    base-path: classpath:config
  cartridges:
    base-path: classpath:cartridges
  cache:
    warm-on-startup: true
  bulk:
    parallelism: 0  # 0=disabled, >1=parallel threads
  visa:
    base-url: https://sandbox.api.visa.com
    path: /visapayouts/v3/payouts
```

## Monitoring

Actuator endpoints available at:

- `/actuator/health` - Health check
- `/actuator/metrics` - Metrics
- `/actuator/prometheus` - Prometheus metrics

## License

MIT
