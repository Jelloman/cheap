# cheap-rag-client Module

This file provides guidance to Claude Code when working with the cheap-rag-client module.

## Module Overview

The cheap-rag-client module provides a Java client library for interacting with the Python-based cheap-rag REST API. It offers a type-safe, fluent interface for performing RAG (Retrieval-Augmented Generation) queries with metadata filtering.

## Key Differences from cheap-rest-client

This module follows the same patterns as cheap-rest-client but targets a different API:

- **Target API**: Python cheap-rag (not cheap-rest)
- **DTOs**: Separate from cheap-json (RAG concepts vs CHEAP domain)
- **JSON Mapping**: Snake case (Python convention) vs camel case (Java)
- **No Domain Dependencies**: Does not depend on cheap-core or cheap-json

## Package Structure

```
net.netbeing.cheap.rag.client/
├── CheapRagClient.java          # Client interface
├── CheapRagClientImpl.java      # WebClient implementation
├── CheapRagClientConfig.java    # Configuration
├── FilterBuilder.java           # Fluent filter API
├── dto/                         # Request/response DTOs
│   ├── QueryRequest.java
│   ├── QueryResponse.java
│   ├── IndexStatusResponse.java
│   ├── SearchMetadata.java
│   ├── GenerationMetadata.java
│   ├── CitationMetrics.java
│   ├── CitationInfo.java
│   └── ArtifactSummary.java
└── exception/                   # Exception hierarchy
    ├── CheapRagClientException.java
    ├── CheapRagBadRequestException.java
    ├── CheapRagNotFoundException.java
    └── CheapRagServerException.java
```

## Development Guidelines

### JSON Mapping

**Critical**: All JSON field names use snake_case to match Python API:

```java
@JsonProperty("artifact_id")
String artifactId;

@JsonProperty("search_metadata")
SearchMetadata searchMetadata;

@JsonProperty("top_k")
Integer topK;
```

ObjectMapper is configured in CheapRagClientImpl:

```java
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
```

### DTOs in Separate Package

Unlike cheap-rest-client, this module defines its own DTOs in the `dto` package:

- **Reason**: RAG concepts (citations, embeddings, search metadata) are semantically different from CHEAP domain (catalogs, hierarchies, aspects)
- **Benefit**: Keeps cheap-json focused on core CHEAP model
- **Trade-off**: No shared DTOs with other modules

### FilterBuilder Pattern

Provides fluent API for building metadata filters:

```java
Map<String, Object> filters = FilterBuilder.create()
    .language("java")
    .type("interface")
    .tags("core", "cheap")
    .custom("nullable", false)
    .build();
```

**Implementation details**:
- Immutable builder pattern
- Returns `Map<String, Object>` for flexibility
- Supports standard fields + custom fields
- No validation (server validates)

### Dual API (Blocking + Reactive)

Provide both blocking and reactive methods:

```java
// Blocking
QueryResponse query(QueryRequest request);

// Reactive
Mono<QueryResponse> queryAsync(QueryRequest request);
```

**Pattern**: Implement reactive first, block for synchronous:

```java
@Override
public QueryResponse query(QueryRequest request) {
    return queryAsync(request).block();
}
```

### WebClient Configuration

Configure WebClient with:
1. Snake case JSON mapping
2. Timeouts from config
3. Connection pooling
4. Custom codecs for JSON

```java
ExchangeStrategies strategies = ExchangeStrategies.builder()
    .codecs(configurer -> {
        configurer.defaultCodecs().jackson2JsonEncoder(
            new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
        configurer.defaultCodecs().jackson2JsonDecoder(
            new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
    })
    .build();
```

### Error Handling

Map HTTP status codes to specific exceptions:

```java
private Mono<? extends Throwable> handleClientError(ClientResponse response) {
    return response.bodyToMono(String.class)
        .flatMap(body -> {
            HttpStatus status = (HttpStatus) response.statusCode();
            return switch (status) {
                case NOT_FOUND -> Mono.error(new CheapRagNotFoundException(body));
                case BAD_REQUEST -> Mono.error(new CheapRagBadRequestException(body));
                default -> Mono.error(new CheapRagClientException(...));
            };
        });
}
```

## Testing Guidelines

### MockWebServer for HTTP Mocking

Use OkHttp MockWebServer for unit tests:

```java
@BeforeEach
void setup() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    String baseUrl = mockWebServer.url("/").toString();
    client = new CheapRagClientImpl(baseUrl);
}

@Test
void testQuery() throws IOException {
    String responseBody = loadTestResource("query-response.json");

    mockWebServer.enqueue(new MockResponse()
        .setBody(responseBody)
        .addHeader("Content-Type", "application/json"));

    QueryResponse response = client.query("test");
    assertNotNull(response);
}
```

### JSON Test Fixtures

Store expected JSON responses in `src/test/resources/http-tests/`:

```
src/test/resources/http-tests/
├── query-response.json
├── index-status-response.json
└── error-response.json
```

**Format**: Must use snake_case matching Python API

### Testing FilterBuilder

Test fluent API and output format:

```java
@Test
void testFilterBuilder() {
    Map<String, Object> filters = FilterBuilder.create()
        .language("java")
        .type("interface")
        .tags("core", "api")
        .build();

    assertEquals("java", filters.get("language"));
    assertEquals("interface", filters.get("type"));
    assertTrue(filters.containsKey("tags"));
}
```

### Testing Error Handling

Test each exception type:

```java
@Test
void testBadRequestError() {
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(400)
        .setBody("Invalid query parameters"));

    assertThrows(CheapRagBadRequestException.class, () -> {
        client.query("test");
    });
}
```

## Common Tasks

### Adding a New Query Parameter

1. Add field to QueryRequest DTO:
   ```java
   @JsonProperty("new_param")
   @Nullable
   Integer newParam;
   ```

2. Update builder usage in README examples

3. Add test case

### Adding a New Filter Type

1. Add method to FilterBuilder:
   ```java
   public FilterBuilder newFilter(String value) {
       filters.put("new_filter", value);
       return this;
   }
   ```

2. Add test case

3. Document in README

### Adding a New Endpoint

1. Add method to CheapRagClient interface:
   ```java
   NewResponse newOperation(NewRequest request);
   ```

2. Implement in CheapRagClientImpl:
   ```java
   @Override
   public NewResponse newOperation(NewRequest request) {
       return webClient.post()
           .uri("/api/new-endpoint")
           .bodyValue(request)
           .retrieve()
           .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
           .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
           .bodyToMono(NewResponse.class)
           .block();
   }
   ```

3. Create DTOs if needed

4. Add tests

5. Update README

## Integration with cheap-rag API

### API Endpoints (Phase 1.3)

- `POST /api/query` - RAG query
- `GET /api/index/status` - Index statistics
- `GET /health` - Health check

### Future Endpoints (Placeholder)

- `POST /api/index/rebuild` - Trigger reindex
- `GET /api/metadata/browse` - Browse with pagination

Implement placeholders as `UnsupportedOperationException` until API is ready.

## Performance Considerations

### WebClient Reuse

The WebClient instance is thread-safe and should be reused:

```java
private final WebClient webClient;

public CheapRagClientImpl(String baseUrl) {
    this.webClient = createWebClient(...);
}
```

### Connection Pooling

Configured via CheapRagClientConfig:

```java
ConnectionProvider provider = ConnectionProvider.builder("cheap-rag-pool")
    .maxConnections(config.getMaxConnections())
    .maxIdleTime(config.getMaxIdleTime())
    .build();
```

### Timeouts

Separate timeouts for connect and response:

```java
HttpClient httpClient = HttpClient.create(provider)
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
    .responseTimeout(Duration.ofSeconds(30));
```

## Dependencies

- **Spring WebFlux**: Reactive HTTP client
- **Jackson**: JSON processing with snake_case mapping
- **SLF4J**: Logging
- **Lombok**: Boilerplate reduction
- **JUnit Jupiter**: Testing
- **MockWebServer**: HTTP mocking

## Related Modules

- `cheap-rag` - Java utilities for cheap-rag (extractor)
- `cheap-rest-client` - Reference pattern for REST clients

## Documentation

See [README.md](README.md) for API usage examples and integration guide.
