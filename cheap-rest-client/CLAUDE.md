# cheap-rest-client Module

This file provides guidance to Claude Code when working with the cheap-rest-client module.

## Module Overview

The cheap-rest-client module provides a Java client library for interacting with the Cheap REST API. It offers a type-safe, fluent interface for all Cheap REST operations using Spring WebClient for HTTP communication.

## Documentation

See [README.md](README.md) for comprehensive API usage documentation, examples, and error handling.

## Package Structure

```
net.netbeing.cheap.rest.client/
├── CheapRestClient.java       # Main client interface
├── CheapRestClientImpl.java   # Client implementation
└── exception/                  # Client-specific exceptions
    ├── CheapRestClientException.java
    ├── CheapRestNotFoundException.java
    ├── CheapRestBadRequestException.java
    └── CheapRestServerException.java
```

## Development Guidelines

### Client Interface Design

The `CheapRestClient` interface defines all operations:

```java
public interface CheapRestClient {
    // Catalog operations
    CreateCatalogResponse createCatalog(CatalogDef catalogDef, CatalogSpecies species, UUID upstream);
    CatalogListResponse listCatalogs(int page, int size);
    CatalogDef getCatalog(UUID catalogId);

    // AspectDef operations
    CreateAspectDefResponse createAspectDef(UUID catalogId, AspectDef aspectDef);
    AspectDefListResponse listAspectDefs(UUID catalogId, int page, int size);
    AspectDef getAspectDef(UUID catalogId, UUID aspectDefId);
    AspectDef getAspectDefByName(UUID catalogId, String name);

    // Aspect operations
    UpsertAspectsResponse upsertAspects(UUID catalogId, String aspectDefName, Map<UUID, Map<String, Object>> aspects);
    AspectQueryResponse queryAspects(UUID catalogId, Set<UUID> entityIds, Set<String> aspectDefNames);

    // Hierarchy operations
    EntityListResponse getEntityList(UUID catalogId, String hierarchyName, int page, int size);
    EntityDirectoryResponse getEntityDirectory(UUID catalogId, String hierarchyName);
    EntityTreeResponse getEntityTree(UUID catalogId, String hierarchyName);
    AspectMapResponse getAspectMap(UUID catalogId, String hierarchyName, int page, int size);
}
```

### Implementation Pattern

Use WebClient for HTTP operations:

```java
@Override
public CatalogDef getCatalog(UUID catalogId) {
    return webClient.get()
        .uri("/api/catalog/{id}", catalogId)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> handleClientError(response)
        )
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> handleServerError(response)
        )
        .bodyToMono(CatalogDef.class)
        .block();
}
```

### Exception Handling

Use specific exception types for different error conditions:

```java
private Mono<? extends Throwable> handleClientError(ClientResponse response) {
    return response.bodyToMono(String.class)
        .flatMap(body -> {
            HttpStatus status = (HttpStatus) response.statusCode();
            return switch (status) {
                case NOT_FOUND -> Mono.error(
                    new CheapRestNotFoundException(body)
                );
                case BAD_REQUEST -> Mono.error(
                    new CheapRestBadRequestException(body)
                );
                default -> Mono.error(
                    new CheapRestClientException(body)
                );
            };
        });
}
```

### WebClient Configuration

Allow custom WebClient configuration:

```java
public CheapRestClientImpl(String baseUrl) {
    this.webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
}

public CheapRestClientImpl(WebClient webClient) {
    this.webClient = webClient;
}
```

## Testing Guidelines

### Unit Tests

Test client methods with MockWebServer or WebTestClient:

```java
@ExtendWith(MockitoExtension.class)
class CheapRestClientImplTest {

    private MockWebServer mockWebServer;
    private CheapRestClient client;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        client = new CheapRestClientImpl(baseUrl);
    }

    @AfterEach
    void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetCatalog() {
        // Mock response
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"550e8400-e29b-41d4-a716-446655440000\"}")
            .addHeader("Content-Type", "application/json"));

        // Test
        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        CatalogDef catalog = client.getCatalog(catalogId);

        assertNotNull(catalog);
        assertEquals(catalogId, catalog.getId());
    }
}
```

### Integration Tests

Test against real cheap-rest service:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CheapRestClientIntegrationTest {

    @LocalServerPort
    private int port;

    private CheapRestClient client;

    @BeforeEach
    void setup() {
        client = new CheapRestClientImpl("http://localhost:" + port);
    }

    @Test
    void testFullCatalogLifecycle() {
        // Create catalog
        CreateCatalogResponse response = client.createCatalog(...);

        // Retrieve catalog
        CatalogDef catalog = client.getCatalog(response.catalogId());

        assertNotNull(catalog);
    }
}
```

### Testing Error Handling

Test all exception types:

```java
@Test
void testNotFoundError() {
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(404)
        .setBody("Catalog not found"));

    UUID catalogId = UUID.randomUUID();

    assertThrows(CheapRestNotFoundException.class, () -> {
        client.getCatalog(catalogId);
    });
}

@Test
void testBadRequestError() {
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(400)
        .setBody("Invalid catalog definition"));

    assertThrows(CheapRestBadRequestException.class, () -> {
        client.createCatalog(null, null, null);
    });
}
```

## Common Tasks

### Adding a New Client Method

1. Add method to `CheapRestClient` interface:
   ```java
   public interface CheapRestClient {
       NewOperationResponse performNewOperation(UUID catalogId, String param);
   }
   ```

2. Implement in `CheapRestClientImpl`:
   ```java
   @Override
   public NewOperationResponse performNewOperation(UUID catalogId, String param) {
       return webClient.post()
           .uri("/api/catalog/{id}/operation", catalogId)
           .bodyValue(Map.of("param", param))
           .retrieve()
           .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
           .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
           .bodyToMono(NewOperationResponse.class)
           .block();
   }
   ```

3. Write tests for the new method

4. Update README.md with usage example

### Adding Request/Response DTOs

DTOs are defined in cheap-json module:

```java
// cheap-json module
public record NewOperationRequest(
    UUID catalogId,
    String param
) {}

public record NewOperationResponse(
    boolean success,
    String message
) {}
```

Use them in client:

```java
@Override
public NewOperationResponse performOperation(NewOperationRequest request) {
    return webClient.post()
        .uri("/api/operation")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(NewOperationResponse.class)
        .block();
}
```

### Customizing WebClient

Allow users to customize WebClient for authentication, logging, etc.:

```java
// Example: Adding authentication
WebClient customClient = WebClient.builder()
    .baseUrl("http://localhost:8080")
    .defaultHeader("Authorization", "Bearer " + token)
    .filter(ExchangeFilterFunctions.basicAuthentication(username, password))
    .build();

CheapRestClient client = new CheapRestClientImpl(customClient);
```

### Adding Retry Logic

Add retry for transient failures:

```java
import org.springframework.retry.support.RetryTemplate;

@Override
public CatalogDef getCatalog(UUID catalogId) {
    RetryTemplate retryTemplate = RetryTemplate.builder()
        .maxAttempts(3)
        .fixedBackoff(1000)
        .retryOn(WebClientException.class)
        .build();

    return retryTemplate.execute(context ->
        webClient.get()
            .uri("/api/catalog/{id}", catalogId)
            .retrieve()
            .bodyToMono(CatalogDef.class)
            .block()
    );
}
```

### Adding Timeout Configuration

Configure timeouts:

```java
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

HttpClient httpClient = HttpClient.create()
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
    .responseTimeout(Duration.ofSeconds(10));

WebClient webClient = WebClient.builder()
    .baseUrl("http://localhost:8080")
    .clientConnector(new ReactorClientHttpConnector(httpClient))
    .build();

CheapRestClient client = new CheapRestClientImpl(webClient);
```

## Performance Considerations

### WebClient Reuse

WebClient is thread-safe and should be reused:

```java
// Good: Single WebClient instance
private final WebClient webClient;

public CheapRestClientImpl(String baseUrl) {
    this.webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build();
}

// Bad: Creating new WebClient for each request
public void someMethod() {
    WebClient client = WebClient.create();  // Don't do this
}
```

### Connection Pooling

WebClient uses Reactor Netty with connection pooling by default. Configure if needed:

```java
import reactor.netty.resources.ConnectionProvider;

ConnectionProvider provider = ConnectionProvider.builder("custom")
    .maxConnections(50)
    .maxIdleTime(Duration.ofSeconds(30))
    .build();

HttpClient httpClient = HttpClient.create(provider);

WebClient webClient = WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(httpClient))
    .build();
```

### Async Operations

For non-blocking operations, use reactive types:

```java
public interface CheapRestClient {
    // Blocking
    CatalogDef getCatalog(UUID catalogId);

    // Non-blocking
    Mono<CatalogDef> getCatalogAsync(UUID catalogId);
}

@Override
public Mono<CatalogDef> getCatalogAsync(UUID catalogId) {
    return webClient.get()
        .uri("/api/catalog/{id}", catalogId)
        .retrieve()
        .bodyToMono(CatalogDef.class);
}
```

## Dependencies

- **cheap-core** - Core data model
- **cheap-json** - JSON DTOs and serialization
- **Spring WebFlux** - Reactive HTTP client
- **Reactor Netty** - HTTP client implementation

## Build Commands

```bash
# Build module
./gradlew :cheap-rest-client:build

# Run tests
./gradlew :cheap-rest-client:test

# Run integration tests (requires cheap-rest running)
./gradlew :cheap-rest-client:integrationTest
```

## Related Modules

- `cheap-core` - Core data model (required)
- `cheap-json` - JSON handling (required)
- `cheap-rest` - REST API service (target of client)

## Example Usage

See README.md for comprehensive usage examples covering:
- Creating and configuring the client
- Catalog operations
- AspectDef operations
- Aspect operations
- Hierarchy operations
- Error handling
- Custom WebClient configuration
