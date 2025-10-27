# Cheap REST WebFlux Migration Plan

## Overview

This document outlines the plan for migrating the cheap-rest module from Spring MVC (blocking, servlet-based) to Spring WebFlux (reactive, non-blocking). This migration will also include adding comprehensive HTTP-based integration tests that use JSON files in the test/resources directory for request and response validation.

## Goals

1. Migrate from Spring Boot Starter Web (MVC) to Spring Boot Starter WebFlux
2. Convert all controllers to use reactive types (Mono, Flux)
3. Wrap blocking JDBC operations appropriately for reactive contexts
4. Add comprehensive JSON-based HTTP integration tests
5. Maintain backward compatibility of the REST API (same endpoints, same request/response formats)
6. Keep all existing unit tests passing

## Current State Analysis

### Current Architecture

**Controllers:**
- Use `@RestController` and `@RequestMapping` annotations
- Return `ResponseEntity<T>` with blocking operations
- Example: `public ResponseEntity<CreateCatalogResponse> createCatalog(...)`

**Services:**
- Annotated with `@Service` and use `@Transactional`
- Perform blocking JDBC operations via `DataSource.getConnection()`
- Throw `SQLException` which is caught and wrapped in `CheapException`
- Use synchronous DAO calls: `dao.saveCatalog(catalog)`, `dao.loadCatalog(catalogId)`

**Dependencies:**
- `spring-boot-starter-web` - Servlet-based blocking web framework
- `spring-boot-starter-jdbc` - Blocking JDBC support
- Synchronous Jackson serialization

**Tests:**
- Service unit tests extend `BaseServiceTest`
- No HTTP-level integration tests currently exist
- Tests are synchronous and use direct service method calls

### Key Challenges

1. **Blocking JDBC:** All database operations use blocking JDBC drivers (PostgreSQL, SQLite, MariaDB)
2. **Transaction Management:** Current use of `@Transactional` won't work the same in reactive contexts
3. **Exception Handling:** Need to adapt exception handling to reactive streams
4. **Testing:** Need new reactive test infrastructure

## Migration Strategy

### Strategy: Wrapper Approach (Recommended)

Since all cheap-db modules use blocking JDBC, we will:

1. Keep the existing blocking DAO implementations unchanged
2. Wrap all blocking operations in `Mono.fromCallable()` with `subscribeOn(Schedulers.boundedElastic())`
3. Use a dedicated thread pool for blocking JDBC operations
4. Convert controllers to return reactive types
5. Update exception handling for reactive contexts

**Rationale:**
- Avoids rewriting all DAO implementations with R2DBC
- Maintains compatibility with all three database backends
- Allows gradual migration if needed
- Provides reactive API benefits (backpressure, non-blocking I/O for HTTP)

**Note:** This is a "reactive wrapper" approach, not "true reactive all the way down." The JDBC operations remain blocking but are executed on a separate thread pool to avoid blocking the reactive event loop.

## Phase 1: Update Dependencies

**Objective:** Replace Spring MVC dependencies with Spring WebFlux.

### Changes to build.gradle.kts

**Remove:**
```kotlin
implementation(libs.spring.boot.starter.web)
```

**Add:**
```kotlin
implementation(libs.spring.boot.starter.webflux)
```

**Keep (unchanged):**
```kotlin
implementation(libs.spring.boot.starter.jdbc)  // Still needed for JDBC operations
implementation(libs.spring.boot.starter.validation)
implementation(libs.spring.boot.starter.actuator)
```

### Update gradle/libs.versions.toml

**Add:**
```toml
[libraries]
spring-boot-starter-webflux = { module = "org.springframework.boot:spring-boot-starter-webflux", version.ref = "spring-boot" }
```

### Validation Checklist

- [ ] Remove spring-boot-starter-web from build.gradle.kts
- [ ] Add spring-boot-starter-webflux to build.gradle.kts
- [ ] Add spring-boot-starter-webflux to libs.versions.toml
- [ ] Run `./gradlew :cheap-rest:dependencies` to verify dependency tree
- [ ] Verify no conflicts between WebFlux and other dependencies

## Phase 2: Create Reactive Service Wrappers

**Objective:** Wrap blocking service operations in reactive types without modifying the underlying services.

### Create Reactive Configuration

**File:** `cheap-rest/src/main/java/net/netbeing/cheap/rest/config/ReactiveConfig.java`

```java
package net.netbeing.cheap.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Configuration for reactive execution.
 * Provides a dedicated scheduler for blocking JDBC operations.
 */
@Configuration
public class ReactiveConfig
{
    /**
     * Creates a bounded elastic scheduler for blocking database operations.
     * This scheduler is designed for blocking I/O and won't block the reactive event loop.
     *
     * @return a Scheduler instance for blocking operations
     */
    @Bean
    public Scheduler jdbcScheduler()
    {
        // Create a bounded elastic scheduler with thread pool for JDBC operations
        // This prevents blocking the reactive event loop
        return Schedulers.boundedElastic();
    }
}
```

### Update Service Layer

**Option A: Create Reactive Service Facades (Recommended)**

Create reactive wrapper services that delegate to existing blocking services:

**File:** `cheap-rest/src/main/java/net/netbeing/cheap/rest/service/ReactiveCatalogService.java`

```java
package net.netbeing.cheap.rest.service;

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Reactive wrapper for CatalogService.
 * Wraps blocking JDBC operations in reactive types using a dedicated scheduler.
 */
@Service
public class ReactiveCatalogService
{
    private final CatalogService catalogService;
    private final Scheduler jdbcScheduler;

    public ReactiveCatalogService(CatalogService catalogService, Scheduler jdbcScheduler)
    {
        this.catalogService = catalogService;
        this.jdbcScheduler = jdbcScheduler;
    }

    /**
     * Creates a new catalog reactively.
     */
    public Mono<UUID> createCatalog(@NotNull CatalogDef catalogDef, @NotNull CatalogSpecies species,
                                    UUID upstream, URI uri)
    {
        return Mono.fromCallable(() -> catalogService.createCatalog(catalogDef, species, upstream, uri))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Lists catalog IDs reactively with pagination.
     */
    public Mono<List<UUID>> listCatalogIds(int page, int size)
    {
        return Mono.fromCallable(() -> catalogService.listCatalogIds(page, size))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Counts all catalogs reactively.
     */
    public Mono<Long> countCatalogs()
    {
        return Mono.fromCallable(() -> catalogService.countCatalogs())
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets a catalog by ID reactively.
     */
    public Mono<Catalog> getCatalog(@NotNull UUID catalogId)
    {
        return Mono.fromCallable(() -> catalogService.getCatalog(catalogId))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets a catalog definition by ID reactively.
     */
    public Mono<CatalogDef> getCatalogDef(@NotNull UUID catalogId)
    {
        return Mono.fromCallable(() -> catalogService.getCatalogDef(catalogId))
            .subscribeOn(jdbcScheduler);
    }
}
```

**Similarly create:**
- `ReactiveAspectDefService.java` - wraps `AspectDefService`
- `ReactiveAspectService.java` - wraps `AspectService`
- `ReactiveHierarchyService.java` - wraps `HierarchyService`

**Option B: Modify Existing Services (Alternative)**

Alternatively, convert existing services to return reactive types directly. This is more invasive but eliminates the wrapper layer.

**Recommendation:** Use Option A (facades) to minimize changes and maintain existing unit tests.

### Validation Checklist

- [ ] Create ReactiveConfig with jdbcScheduler bean
- [ ] Create ReactiveCatalogService wrapper
- [ ] Create ReactiveAspectDefService wrapper
- [ ] Create ReactiveAspectService wrapper
- [ ] Create ReactiveHierarchyService wrapper
- [ ] Verify all reactive services properly delegate to blocking services
- [ ] Verify all operations use subscribeOn(jdbcScheduler)

## Phase 3: Convert Controllers to Reactive

**Objective:** Update all controllers to use reactive types and reactive service facades.

### Example: CatalogController Conversion

**Before (MVC):**
```java
@PostMapping
public ResponseEntity<CreateCatalogResponse> createCatalog(@RequestBody CreateCatalogRequest request)
{
    UUID catalogId = catalogService.createCatalog(
        request.catalogDef(),
        request.species(),
        request.upstream(),
        request.uri()
    );

    CreateCatalogResponse response = new CreateCatalogResponse(
        catalogId,
        request.uri(),
        "Catalog created successfully"
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**After (WebFlux):**
```java
@PostMapping
public Mono<ResponseEntity<CreateCatalogResponse>> createCatalog(@RequestBody CreateCatalogRequest request)
{
    return reactiveCatalogService.createCatalog(
            request.catalogDef(),
            request.species(),
            request.upstream(),
            request.uri()
        )
        .map(catalogId -> {
            CreateCatalogResponse response = new CreateCatalogResponse(
                catalogId,
                request.uri(),
                "Catalog created successfully"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        });
}
```

**Alternative (more concise):**
```java
@PostMapping
public Mono<CreateCatalogResponse> createCatalog(@RequestBody CreateCatalogRequest request)
{
    return reactiveCatalogService.createCatalog(
            request.catalogDef(),
            request.species(),
            request.upstream(),
            request.uri()
        )
        .map(catalogId -> new CreateCatalogResponse(
            catalogId,
            request.uri(),
            "Catalog created successfully"
        ));
}
```

**Note:** WebFlux automatically sets the HTTP status to 200 OK by default. Use `@ResponseStatus(HttpStatus.CREATED)` or return `ResponseEntity` for custom status codes.

### Controller Conversion Checklist

**CatalogController:**
- [ ] Inject `ReactiveCatalogService` instead of `CatalogService`
- [ ] Convert `createCatalog()` to return `Mono<ResponseEntity<CreateCatalogResponse>>`
- [ ] Convert `listCatalogs()` to return `Mono<CatalogListResponse>`
- [ ] Convert `getCatalog()` to return `Mono<CatalogDef>`

**AspectDefController:**
- [ ] Inject `ReactiveAspectDefService`
- [ ] Convert `createAspectDef()` to return `Mono<ResponseEntity<CreateAspectDefResponse>>`
- [ ] Convert `listAspectDefs()` to return `Mono<AspectDefListResponse>`
- [ ] Convert `getAspectDef()` to return `Mono<AspectDef>`

**AspectController:**
- [ ] Inject `ReactiveAspectService`
- [ ] Convert `upsertAspects()` to return `Mono<ResponseEntity<UpsertAspectsResponse>>`
- [ ] Convert `queryAspects()` to return `Mono<AspectQueryResponse>`

**HierarchyController:**
- [ ] Inject `ReactiveHierarchyService`
- [ ] Convert all hierarchy retrieval methods to return `Mono<T>`

### Exception Handling

Create a reactive exception handler:

**File:** `cheap-rest/src/main/java/net/netbeing/cheap/rest/exception/ReactiveExceptionHandler.java`

```java
package net.netbeing.cheap.rest.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for reactive controllers.
 */
@RestControllerAdvice
public class ReactiveExceptionHandler
{
    private static final Logger logger = LoggerFactory.getLogger(ReactiveExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResourceNotFound(ResourceNotFoundException ex)
    {
        logger.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ValidationErrorResponse>> handleValidation(ValidationException ex)
    {
        logger.warn("Validation error: {}", ex.getMessage());
        ValidationErrorResponse error = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            ex.getErrors()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnprocessableEntity(UnprocessableEntityException ex)
    {
        logger.warn("Unprocessable entity: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            ex.getMessage()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex)
    {
        logger.error("Unexpected error", ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred: " + ex.getMessage()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }

    /**
     * Standard error response DTO.
     */
    public record ErrorResponse(int status, String message)
    {
    }

    /**
     * Validation error response with detailed field errors.
     */
    public record ValidationErrorResponse(
        int status,
        String message,
        java.util.List<ValidationException.ValidationError> errors)
    {
    }
}
```

### Validation Checklist

- [ ] All controllers converted to reactive types
- [ ] All methods return Mono<T> or Flux<T>
- [ ] ReactiveExceptionHandler created and tested
- [ ] Error responses maintain same format as before
- [ ] HTTP status codes remain the same

## Phase 4: Add JSON-Based HTTP Integration Tests

**Objective:** Create comprehensive HTTP-level integration tests using JSON files for requests and expected responses.

### Test Directory Structure

```
cheap-rest/src/test/
├── java/net/netbeing/cheap/rest/
│   ├── controller/
│   │   ├── CatalogControllerHttpTest.java
│   │   ├── AspectDefControllerHttpTest.java
│   │   ├── AspectControllerHttpTest.java
│   │   └── HierarchyControllerHttpTest.java
│   └── service/
│       └── (existing service tests remain unchanged)
└── resources/
    └── http-tests/
        ├── catalog/
        │   ├── create-catalog-request.json
        │   ├── create-catalog-response.json
        │   ├── list-catalogs-response.json
        │   ├── get-catalog-response.json
        │   └── invalid-catalog-request.json
        ├── aspectdef/
        │   ├── create-aspectdef-request.json
        │   ├── create-aspectdef-response.json
        │   ├── list-aspectdefs-response.json
        │   └── get-aspectdef-response.json
        ├── aspect/
        │   ├── upsert-aspects-request.json
        │   ├── upsert-aspects-response-success.json
        │   ├── upsert-aspects-response-partial.json
        │   ├── query-aspects-request.json
        │   └── query-aspects-response.json
        └── hierarchy/
            ├── entity-list-response.json
            ├── entity-directory-response.json
            ├── entity-tree-response.json
            └── aspect-map-response.json
```

### Base Test Class

**File:** `cheap-rest/src/test/java/net/netbeing/cheap/rest/controller/BaseControllerHttpTest.java`

```java
package net.netbeing.cheap.rest.controller;

import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Base class for HTTP integration tests using WebTestClient.
 * Provides utilities for loading JSON test files and common test setup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public abstract class BaseControllerHttpTest
{
    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected CheapDao dao;

    @Autowired
    protected CheapFactory factory;

    @BeforeEach
    void setUp() throws SQLException
    {
        // Clear database before each test
        // Implementation depends on your database schema
        // For SQLite: DELETE FROM all tables
    }

    @AfterEach
    void tearDown() throws SQLException
    {
        // Additional cleanup if needed
        factory.clearEntityRegistry();
    }

    /**
     * Loads a JSON file from test/resources/http-tests directory.
     *
     * @param relativePath path relative to http-tests directory (e.g., "catalog/create-catalog-request.json")
     * @return JSON content as a string
     */
    protected String loadJson(String relativePath) throws IOException
    {
        Path path = Paths.get("src/test/resources/http-tests", relativePath);
        return Files.readString(path);
    }

    /**
     * Normalizes JSON by removing whitespace differences for comparison.
     * Parses and re-serializes to ensure consistent formatting.
     */
    protected String normalizeJson(String json) throws IOException
    {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Object obj = mapper.readValue(json, Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
}
```

### Example Test Class

**File:** `cheap-rest/src/test/java/net/netbeing/cheap/rest/controller/CatalogControllerHttpTest.java`

```java
package net.netbeing.cheap.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP integration tests for CatalogController using JSON files.
 */
class CatalogControllerHttpTest extends BaseControllerHttpTest
{
    @Test
    void testCreateCatalog() throws Exception
    {
        // Load request JSON
        String requestJson = loadJson("catalog/create-catalog-request.json");

        // Send POST request and verify response
        String responseJson = webTestClient.post()
            .uri("/api/catalog")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Load expected response template
        String expectedTemplate = loadJson("catalog/create-catalog-response.json");

        // Parse and verify structure (ignoring dynamic fields like catalogId)
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode actualNode = mapper.readTree(responseJson);
        com.fasterxml.jackson.databind.JsonNode expectedNode = mapper.readTree(expectedTemplate);

        // Verify static fields
        assertThat(actualNode.get("message").asText())
            .isEqualTo(expectedNode.get("message").asText());

        // Verify dynamic fields exist and have correct format
        assertThat(actualNode.has("catalogId")).isTrue();
        assertThat(actualNode.get("catalogId").asText()).matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );
    }

    @Test
    void testListCatalogs() throws Exception
    {
        // First create a catalog
        String createRequest = loadJson("catalog/create-catalog-request.json");
        webTestClient.post()
            .uri("/api/catalog")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated();

        // Then list catalogs
        String responseJson = webTestClient.get()
            .uri("/api/catalog?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response structure
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode responseNode = mapper.readTree(responseJson);

        assertThat(responseNode.has("catalogIds")).isTrue();
        assertThat(responseNode.has("page")).isTrue();
        assertThat(responseNode.has("size")).isTrue();
        assertThat(responseNode.has("totalElements")).isTrue();
        assertThat(responseNode.has("totalPages")).isTrue();

        // Verify pagination values
        assertThat(responseNode.get("page").asInt()).isEqualTo(0);
        assertThat(responseNode.get("size").asInt()).isEqualTo(20);
        assertThat(responseNode.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testGetCatalog() throws Exception
    {
        // Create a catalog
        String createRequest = loadJson("catalog/create-catalog-request.json");
        String createResponse = webTestClient.post()
            .uri("/api/catalog")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Extract catalog ID
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String catalogId = mapper.readTree(createResponse).get("catalogId").asText();

        // Get the catalog
        String responseJson = webTestClient.get()
            .uri("/api/catalog/" + catalogId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Load expected response and compare (ignoring UUIDs)
        String expectedJson = loadJson("catalog/get-catalog-response.json");

        com.fasterxml.jackson.databind.JsonNode actualNode = mapper.readTree(responseJson);
        com.fasterxml.jackson.databind.JsonNode expectedNode = mapper.readTree(expectedJson);

        // Verify structure matches (compare hierarchyDefs and aspectDefs arrays)
        assertThat(actualNode.has("hierarchyDefs")).isTrue();
        assertThat(actualNode.has("aspectDefs")).isTrue();
    }

    @Test
    void testCreateCatalogWithInvalidData() throws Exception
    {
        // Load invalid request
        String invalidRequest = loadJson("catalog/invalid-catalog-request.json");

        // Send request and expect 400 Bad Request
        webTestClient.post()
            .uri("/api/catalog")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.message").exists();
    }

    @Test
    void testGetNonExistentCatalog() throws Exception
    {
        // Try to get a catalog that doesn't exist
        String fakeId = "00000000-0000-0000-0000-000000000000";

        webTestClient.get()
            .uri("/api/catalog/" + fakeId)
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.message").exists();
    }
}
```

### Example JSON Test Files

**File:** `cheap-rest/src/test/resources/http-tests/catalog/create-catalog-request.json`

```json
{
  "catalogDef": {
    "hierarchyDefs": [
      {
        "name": "people",
        "type": "ENTITY_SET"
      }
    ],
    "aspectDefs": [
      {
        "name": "person",
        "globalId": "550e8400-e29b-41d4-a716-446655440001",
        "propertyDefs": [
          {
            "name": "firstName",
            "type": "String",
            "isNullable": false,
            "isReadable": true,
            "isWritable": true,
            "isSearchable": false,
            "isUnique": false
          },
          {
            "name": "lastName",
            "type": "String",
            "isNullable": false,
            "isReadable": true,
            "isWritable": true,
            "isSearchable": false,
            "isUnique": false
          },
          {
            "name": "age",
            "type": "Integer",
            "isNullable": true,
            "isReadable": true,
            "isWritable": true,
            "isSearchable": false,
            "isUnique": false
          }
        ]
      }
    ]
  },
  "species": "SINK",
  "upstream": null,
  "uri": "http://example.com/test-catalog"
}
```

**File:** `cheap-rest/src/test/resources/http-tests/catalog/create-catalog-response.json`

```json
{
  "catalogId": "550e8400-e29b-41d4-a716-446655440000",
  "uri": "http://example.com/test-catalog",
  "message": "Catalog created successfully"
}
```

**File:** `cheap-rest/src/test/resources/http-tests/catalog/get-catalog-response.json`

```json
{
  "hierarchyDefs": [
    {
      "name": "people",
      "type": "ENTITY_SET"
    }
  ],
  "aspectDefs": [
    {
      "name": "person",
      "globalId": "550e8400-e29b-41d4-a716-446655440001",
      "propertyDefs": [
        {
          "name": "firstName",
          "type": "String",
          "isNullable": false,
          "isReadable": true,
          "isWritable": true,
          "isSearchable": false,
          "isUnique": false
        },
        {
          "name": "lastName",
          "type": "String",
          "isNullable": false,
          "isReadable": true,
          "isWritable": true,
          "isSearchable": false,
          "isUnique": false
        },
        {
          "name": "age",
          "type": "Integer",
          "isNullable": true,
          "isReadable": true,
          "isWritable": true,
          "isSearchable": false,
          "isUnique": false
        }
      ]
    }
  ]
}
```

**File:** `cheap-rest/src/test/resources/http-tests/catalog/invalid-catalog-request.json`

```json
{
  "catalogDef": {
    "hierarchyDefs": [
      {
        "name": "",
        "type": "ENTITY_SET"
      }
    ],
    "aspectDefs": []
  },
  "species": "SINK",
  "upstream": null,
  "uri": null
}
```

**File:** `cheap-rest/src/test/resources/http-tests/aspect/upsert-aspects-request.json`

```json
{
  "aspects": [
    {
      "entityId": "660e8400-e29b-41d4-a716-446655440001",
      "properties": {
        "firstName": "Alice",
        "lastName": "Smith",
        "age": 30
      }
    },
    {
      "entityId": "660e8400-e29b-41d4-a716-446655440002",
      "properties": {
        "firstName": "Bob",
        "lastName": "Johnson",
        "age": 25
      }
    }
  ]
}
```

**File:** `cheap-rest/src/test/resources/http-tests/aspect/upsert-aspects-response-success.json`

```json
{
  "catalogId": "550e8400-e29b-41d4-a716-446655440000",
  "aspectDefName": "person",
  "results": [
    {
      "entityId": "660e8400-e29b-41d4-a716-446655440001",
      "success": true,
      "created": true,
      "message": "Aspect created"
    },
    {
      "entityId": "660e8400-e29b-41d4-a716-446655440002",
      "success": true,
      "created": true,
      "message": "Aspect created"
    }
  ],
  "successCount": 2,
  "failureCount": 0
}
```

### Test Coverage Requirements

**Each controller should have tests for:**

1. **Happy Path:**
   - Create/upsert operations with valid data
   - Read operations returning expected data
   - List operations with pagination
   - Query operations with filters

2. **Error Cases:**
   - 400 Bad Request - Invalid input data
   - 404 Not Found - Resource doesn't exist
   - 422 Unprocessable Entity - Business logic errors
   - 207 Multi-Status - Partial success in batch operations

3. **Edge Cases:**
   - Empty lists
   - Pagination boundaries (first page, last page, beyond last page)
   - Large payloads
   - Special characters in string fields
   - Null vs. missing fields

### Validation Checklist

- [ ] BaseControllerHttpTest created with JSON loading utilities
- [ ] CatalogControllerHttpTest created with all endpoints covered
- [ ] AspectDefControllerHttpTest created
- [ ] AspectControllerHttpTest created
- [ ] HierarchyControllerHttpTest created
- [ ] All JSON test files created and formatted (pretty-printed)
- [ ] Tests cover happy paths, error cases, and edge cases
- [ ] All HTTP status codes verified
- [ ] Response structure validated against expected JSON
- [ ] Dynamic fields (UUIDs, timestamps) handled appropriately
- [ ] All tests pass with `./gradlew :cheap-rest:test`

## Phase 5: Update Application Configuration

**Objective:** Configure the application for reactive execution.

### Update application.properties

**File:** `cheap-rest/src/main/resources/application.properties`

```properties
# Server configuration (WebFlux uses Netty by default)
server.port=8080

# Pagination defaults
cheap.pagination.default-page-size=20
cheap.pagination.max-page-size=100

# Batch operation limits
cheap.batch.max-size=1000

# Reactor configuration
# Max threads for blocking operations (JDBC)
reactor.schedulers.boundedElastic.max-threads=100
reactor.schedulers.boundedElastic.queue-size=100000
reactor.schedulers.boundedElastic.ttl-seconds=60
```

### Test Configuration

**File:** `cheap-rest/src/test/resources/application-test.properties`

```properties
# Use SQLite for tests
cheap.database.type=sqlite

# In-memory database
spring.datasource.url=jdbc:sqlite::memory:
spring.datasource.driver-class-name=org.sqlite.JDBC

# Flyway migrations
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration/sqlite

# Logging
logging.level.net.netbeing.cheap=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Validation Checklist

- [ ] application.properties configured for WebFlux
- [ ] application-test.properties created for testing
- [ ] Reactor scheduler settings configured appropriately
- [ ] Database connection settings verified
- [ ] Logging configured for debugging

## Phase 6: Update Existing Tests

**Objective:** Ensure existing service unit tests continue to pass.

### Service Tests

**No changes required** for existing service tests since:
- Service layer remains synchronous (blocking)
- Reactive wrappers are in separate classes
- Existing tests extend `BaseServiceTest` and test services directly

### Validation Checklist

- [ ] All existing service tests pass unchanged
- [ ] CatalogServiceTest passes
- [ ] AspectDefServiceTest passes
- [ ] AspectServiceTest passes
- [ ] HierarchyServiceTest passes

## Phase 7: Documentation and OpenAPI

**Objective:** Update API documentation for reactive endpoints.

### OpenAPI Configuration

WebFlux supports OpenAPI/Swagger with minimal changes:

**File:** `cheap-rest/src/main/java/net/netbeing/cheap/rest/config/OpenApiConfig.java`

**Update dependency in build.gradle.kts:**
```kotlin
// Remove: openapi-starter-ui (MVC version)
implementation(libs.openapi.starter.ui)

// Add: openapi-starter-webflux-ui (WebFlux version)
implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.13")
```

**Update libs.versions.toml:**
```toml
openapi-starter-webflux-ui = { module = "org.springdoc:springdoc-openapi-starter-webflux-ui", version.ref = "openapi-starter-ui" }
```

### Validation Checklist

- [ ] OpenAPI dependency updated to WebFlux version
- [ ] Swagger UI accessible at http://localhost:8080/swagger-ui.html
- [ ] All endpoints appear in Swagger UI
- [ ] Request/response schemas correctly generated
- [ ] "Try it out" functionality works

## Implementation Checklist

### Phase 1: Dependencies
- [ ] Remove spring-boot-starter-web
- [ ] Add spring-boot-starter-webflux
- [ ] Update libs.versions.toml
- [ ] Verify dependency tree

### Phase 2: Reactive Services
- [ ] Create ReactiveConfig with jdbcScheduler
- [ ] Create ReactiveCatalogService
- [ ] Create ReactiveAspectDefService
- [ ] Create ReactiveAspectService
- [ ] Create ReactiveHierarchyService
- [ ] Verify all services use subscribeOn(jdbcScheduler)

### Phase 3: Reactive Controllers
- [ ] Update CatalogController
- [ ] Update AspectDefController
- [ ] Update AspectController
- [ ] Update HierarchyController
- [ ] Create ReactiveExceptionHandler
- [ ] Verify HTTP status codes

### Phase 4: HTTP Tests
- [ ] Create test directory structure
- [ ] Create BaseControllerHttpTest
- [ ] Create all JSON test files
- [ ] Implement CatalogControllerHttpTest
- [ ] Implement AspectDefControllerHttpTest
- [ ] Implement AspectControllerHttpTest
- [ ] Implement HierarchyControllerHttpTest
- [ ] Verify all tests pass

### Phase 5: Configuration
- [ ] Update application.properties
- [ ] Create application-test.properties
- [ ] Configure reactor schedulers
- [ ] Verify database connections

### Phase 6: Existing Tests
- [ ] Run all service tests
- [ ] Fix any broken tests
- [ ] Verify test coverage maintained

### Phase 7: Documentation
- [ ] Update OpenAPI dependency
- [ ] Verify Swagger UI works
- [ ] Update README if needed
- [ ] Document reactive behavior

## Testing Strategy

### Unit Tests (Service Layer)
- Keep existing synchronous service tests
- Test blocking services in isolation
- No changes required

### Integration Tests (HTTP Layer)
- New WebTestClient-based tests
- JSON file-driven test cases
- Cover all endpoints and error cases

### Manual Testing
1. Start application: `./gradlew :cheap-rest:bootRun`
2. Access Swagger UI: http://localhost:8080/swagger-ui.html
3. Test each endpoint via Swagger UI
4. Verify responses match expected JSON schemas

## Rollback Plan

If migration encounters issues:

1. **Revert dependencies:** Change back to spring-boot-starter-web
2. **Keep reactive services:** Can co-exist with blocking controllers
3. **Gradual migration:** Convert one controller at a time
4. **Feature flag:** Use profiles to switch between MVC and WebFlux

## Performance Considerations

### Expected Benefits
- Better resource utilization for I/O-bound operations
- Improved scalability under high concurrency
- Non-blocking HTTP request handling

### Limitations
- JDBC operations remain blocking (executed on bounded elastic scheduler)
- No true end-to-end reactive stream for database operations
- Thread pool overhead for blocking operations

### Future Optimizations
1. **R2DBC Migration:** Convert to reactive database drivers
2. **Caching:** Add reactive caching layer
3. **Streaming:** Support streaming large result sets
4. **Connection Pooling:** Optimize JDBC connection pool for reactive context

## Success Criteria

- [ ] All controllers return reactive types (Mono/Flux)
- [ ] All existing service tests pass
- [ ] All new HTTP integration tests pass
- [ ] Application starts successfully with WebFlux
- [ ] Swagger UI displays all endpoints correctly
- [ ] Manual testing via Swagger UI works
- [ ] No regression in API behavior
- [ ] Performance metrics comparable or better than MVC
- [ ] All JSON test files are pretty-printed and well-organized
- [ ] Test coverage > 80% for controllers

## Timeline Estimate

- **Phase 1 (Dependencies):** 1 hour
- **Phase 2 (Reactive Services):** 4-6 hours
- **Phase 3 (Controllers):** 4-6 hours
- **Phase 4 (HTTP Tests):** 8-12 hours
- **Phase 5 (Configuration):** 1-2 hours
- **Phase 6 (Existing Tests):** 2-3 hours
- **Phase 7 (Documentation):** 1-2 hours

**Total:** 21-32 hours

## References

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Project Reactor Reference](https://projectreactor.io/docs/core/release/reference/)
- [Spring Boot WebFlux Testing](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-webflux-tests)
- [Blocking Calls in Reactive Applications](https://projectreactor.io/docs/core/release/reference/#faq.wrap-blocking)
