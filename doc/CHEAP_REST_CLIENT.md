# Cheap REST Client Module Plan

## Overview

This document outlines the plan for creating a new Gradle module `cheap-rest-client`, which will provide a Java library for calling the cheap-rest REST APIs. The client will use Spring Boot WebClient and provide methods that work with Cheap core objects (Catalog, Hierarchy, Entity, Aspect, etc.) rather than raw HTTP/JSON.

## Goals

1. Create a reusable REST client library for the cheap-rest service
2. Provide a clean, type-safe API that uses Cheap core model objects
3. Handle HTTP communication, serialization/deserialization, and error handling
4. Support all endpoints provided by cheap-rest service
5. Maintain separation of concerns (client should not depend on cheap-rest server code)

## Architecture

### Module Dependencies

```
cheap-rest-client
├── cheap-core (api)
└── cheap-json (api)
    ├── cheap-core (api)
    └── Jackson libraries
```

**Key Decision:** The cheap-rest-client module will **NOT** depend on cheap-rest. Instead:
- DTOs will be moved from cheap-rest to cheap-json
- Both cheap-rest and cheap-rest-client will depend on cheap-json
- This ensures clean separation between client and server implementations

### Package Structure

```
net.netbeing.cheap.rest.client/
├── CheapRestClient          # Main client interface
├── CheapRestClientImpl      # WebClient-based implementation
├── config/
│   └── CheapRestClientConfig    # Spring configuration
├── exception/
│   ├── CheapRestClientException     # Base exception
│   ├── CheapRestNotFoundException   # 404 errors
│   ├── CheapRestBadRequestException # 400 errors
│   └── CheapRestServerException     # 500 errors
└── util/
    └── ResponseHandler      # Common response handling logic
```

## Phase 1: Move DTOs from cheap-rest to cheap-json

**Objective:** Relocate all DTO classes to cheap-json so they can be shared between client and server.

### DTOs to Move (13 classes)

From: `cheap-rest/src/main/java/net/netbeing/cheap/rest/dto/`
To: `cheap-json/src/main/java/net/netbeing/cheap/json/dto/`

**Catalog DTOs:**
1. `CreateCatalogRequest.java`
2. `CreateCatalogResponse.java`
3. `CatalogListResponse.java`

**AspectDef DTOs:**
4. `CreateAspectDefResponse.java`
5. `AspectDefListResponse.java`

**Aspect DTOs:**
6. `UpsertAspectsRequest.java`
7. `UpsertAspectsResponse.java` (includes nested `AspectResult` record)
8. `AspectQueryRequest.java`
9. `AspectQueryResponse.java`

**Hierarchy DTOs:**
10. `EntityListResponse.java`
11. `EntityDirectoryResponse.java`
12. `EntityTreeResponse.java`
13. `AspectMapResponse.java`

### Changes Required

1. **Create new package in cheap-json:**
   - `cheap-json/src/main/java/net/netbeing/cheap/json/dto/`

2. **Move all DTO files:**
   - Copy files from cheap-rest to cheap-json
   - Update package declarations from `net.netbeing.cheap.rest.dto` to `net.netbeing.cheap.json.dto`
   - Delete original files from cheap-rest

3. **Update cheap-rest imports:**
   - Find all imports of `net.netbeing.cheap.rest.dto.*` in cheap-rest module
   - Replace with `net.netbeing.cheap.json.dto.*`
   - Files to update:
     - `CatalogController.java`
     - `AspectDefController.java`
     - `HierarchyController.java`
     - `AspectController.java`
     - All service classes that use DTOs
     - All test classes that use DTOs

4. **Test the changes:**
   - Run `./gradlew :cheap-rest:build` to ensure all imports resolve
   - Run `./gradlew :cheap-rest:test` to verify tests pass
   - Run `./gradlew :cheap-json:build` to ensure DTOs compile

### Validation Checklist

- [ ] All 13 DTO files moved to cheap-json
- [ ] Package declarations updated in all DTO files
- [ ] All imports updated in cheap-rest controllers
- [ ] All imports updated in cheap-rest services
- [ ] All imports updated in cheap-rest tests
- [ ] cheap-json builds successfully
- [ ] cheap-rest builds successfully
- [ ] All cheap-rest tests pass

## Phase 2: Create cheap-rest-client Module

**Objective:** Set up the new Gradle module with proper configuration.

### Module Setup

1. **Create directory structure:**
   ```
   cheap-rest-client/
   ├── src/
   │   ├── main/
   │   │   ├── java/
   │   │   │   └── net/netbeing/cheap/rest/client/
   │   │   └── resources/
   │   └── test/
   │       ├── java/
   │       │   └── net/netbeing/cheap/rest/client/
   │       └── resources/
   └── build.gradle.kts
   ```

2. **Create build.gradle.kts:**
   ```kotlin
   /*
    * Copyright (c) 2025. David Noha
    *
    *  Licensed under the Apache License, Version 2.0 (the "License");
    *  you may not use this file except in compliance with the License.
    *  You may obtain a copy of the License at
    *
    *  http://www.apache.org/licenses/LICENSE-2.0
    *
    *  Unless required by applicable law or agreed to in writing, software
    *  distributed under the License is distributed on an "AS IS" BASIS,
    *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    *  See the License for the specific language governing permissions and
    *  limitations under the License.
    */

   /*
    * Build configuration for cheap-rest-client module.
    * This module provides a REST client for the cheap-rest API.
    */

   plugins {
       `java-library`
       idea
       id("io.freefair.lombok") version "8.14.2"
   }

   group = "net.netbeing"
   version = "0.1"

   repositories {
       mavenCentral()
   }

   dependencies {
       // Cheap modules
       api(project(":cheap-core"))
       api(project(":cheap-json"))

       // Spring WebClient (reactive, non-blocking HTTP client)
       implementation(libs.spring.boot.starter.webflux)

       // Logging
       implementation(libs.slf4j)
       implementation(libs.logback.core)
       implementation(libs.logback.classic)

       // Guava
       implementation(libs.guava)

       // JetBrains annotations
       compileOnly(libs.jetbrains.annotations)

       // Testing (unit tests only - integration tests will be in separate module)
       testImplementation(libs.junit.jupiter)
       testImplementation(libs.spring.boot.starter.test)

       testRuntimeOnly(libs.junit.platform.launcher)
   }

   java {
       modularity.inferModulePath = true
       toolchain {
           languageVersion = JavaLanguageVersion.of(24)
       }
   }

   idea {
       module {
           isDownloadJavadoc = true
       }
   }

   tasks.named<Test>("test") {
       useJUnitPlatform()
       jvmArgs("--enable-native-access=ALL-UNNAMED")
   }

   gradle.projectsEvaluated {
       tasks.withType<JavaCompile> {
           options.compilerArgs.addAll(listOf("-Xlint:unchecked"))
       }
   }
   ```

3. **Update settings.gradle.kts:**
   ```kotlin
   rootProject.name = "cheap"
   include("cheap-core")
   include("cheap-json")
   include("cheap-db-postgres")
   include("cheap-db-sqlite")
   include("cheap-db-mariadb")
   include("cheap-rest")
   include("cheap-rest-client")  // Add this line
   ```

4. **Update gradle/libs.versions.toml:**
   - Verify `spring.boot.starter.webflux` is defined
   - If not, add it to the libraries section

### Validation Checklist

- [ ] Directory structure created
- [ ] build.gradle.kts created with correct dependencies
- [ ] settings.gradle.kts updated to include cheap-rest-client
- [ ] `./gradlew :cheap-rest-client:build` succeeds
- [ ] Module appears in `./gradlew projects` output

## Phase 3: Implement Client Interface and Core Classes

**Objective:** Define the main client API and implement core infrastructure.

### CheapRestClient Interface

The main client interface will provide methods organized by resource type:

```java
package net.netbeing.cheap.rest.client;

import net.netbeing.cheap.model.*;
import net.netbeing.cheap.json.dto.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * REST client for interacting with the Cheap REST API.
 * Provides methods for managing catalogs, aspect definitions, aspects, and hierarchies.
 */
public interface CheapRestClient
{
    // ========== Catalog Operations ==========

    /**
     * Creates a new catalog.
     *
     * @param catalogDef the catalog definition
     * @param species the catalog species
     * @param upstream optional upstream catalog ID
     * @param uri optional catalog URI
     * @return the create catalog response
     */
    @NotNull
    CreateCatalogResponse createCatalog(
        @NotNull CatalogDef catalogDef,
        @NotNull CatalogSpecies species,
        UUID upstream,
        java.net.URI uri
    );

    /**
     * Lists catalogs with pagination.
     *
     * @param page page number (0-based)
     * @param size page size
     * @return the catalog list response
     */
    @NotNull
    CatalogListResponse listCatalogs(int page, int size);

    /**
     * Gets a single catalog by ID.
     *
     * @param catalogId the catalog ID
     * @return the catalog definition
     */
    @NotNull
    CatalogDef getCatalog(@NotNull UUID catalogId);

    // ========== AspectDef Operations ==========

    /**
     * Creates a new aspect definition in a catalog.
     *
     * @param catalogId the catalog ID
     * @param aspectDef the aspect definition
     * @return the create aspect def response
     */
    @NotNull
    CreateAspectDefResponse createAspectDef(
        @NotNull UUID catalogId,
        @NotNull AspectDef aspectDef
    );

    /**
     * Lists aspect definitions in a catalog with pagination.
     *
     * @param catalogId the catalog ID
     * @param page page number (0-based)
     * @param size page size
     * @return the aspect def list response
     */
    @NotNull
    AspectDefListResponse listAspectDefs(@NotNull UUID catalogId, int page, int size);

    /**
     * Gets a single aspect definition by ID.
     *
     * @param catalogId the catalog ID
     * @param aspectDefId the aspect def ID
     * @return the aspect definition
     */
    @NotNull
    AspectDef getAspectDef(@NotNull UUID catalogId, @NotNull UUID aspectDefId);

    /**
     * Gets a single aspect definition by name.
     *
     * @param catalogId the catalog ID
     * @param aspectDefName the aspect def name
     * @return the aspect definition
     */
    @NotNull
    AspectDef getAspectDefByName(@NotNull UUID catalogId, @NotNull String aspectDefName);

    // ========== Aspect Operations ==========

    /**
     * Upserts (creates or updates) aspects for entities.
     *
     * @param catalogId the catalog ID
     * @param aspectDefName the aspect definition name
     * @param aspects map of entity ID to property values
     * @return the upsert aspects response
     */
    @NotNull
    UpsertAspectsResponse upsertAspects(
        @NotNull UUID catalogId,
        @NotNull String aspectDefName,
        @NotNull Map<UUID, Map<String, Object>> aspects
    );

    /**
     * Queries aspects for multiple entities and aspect definitions.
     *
     * @param catalogId the catalog ID
     * @param entityIds set of entity IDs to query
     * @param aspectDefNames set of aspect definition names to query
     * @return the aspect query response
     */
    @NotNull
    AspectQueryResponse queryAspects(
        @NotNull UUID catalogId,
        @NotNull Set<UUID> entityIds,
        @NotNull Set<String> aspectDefNames
    );

    // ========== Hierarchy Operations ==========

    /**
     * Gets the contents of an EntityList or EntitySet hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param page page number (0-based)
     * @param size page size
     * @return the entity list response
     */
    @NotNull
    EntityListResponse getEntityList(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        int page,
        int size
    );

    /**
     * Gets the contents of an EntityDirectory hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @return the entity directory response
     */
    @NotNull
    EntityDirectoryResponse getEntityDirectory(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName
    );

    /**
     * Gets the contents of an EntityTree hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @return the entity tree response
     */
    @NotNull
    EntityTreeResponse getEntityTree(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName
    );

    /**
     * Gets the contents of an AspectMap hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param page page number (0-based)
     * @param size page size
     * @return the aspect map response
     */
    @NotNull
    AspectMapResponse getAspectMap(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        int page,
        int size
    );
}
```

### CheapRestClientImpl Implementation

```java
package net.netbeing.cheap.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.netbeing.cheap.json.dto.*;
import net.netbeing.cheap.json.jackson.CheapJacksonDeserializer;
import net.netbeing.cheap.json.jackson.CheapJacksonSerializer;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.rest.client.exception.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Implementation of CheapRestClient using Spring WebClient.
 */
@Slf4j
public class CheapRestClientImpl implements CheapRestClient
{
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final CheapJacksonDeserializer deserializer;

    /**
     * Creates a new CheapRestClient with the specified base URL.
     *
     * @param baseUrl the base URL of the cheap-rest service (e.g., "http://localhost:8080")
     */
    public CheapRestClientImpl(@NotNull String baseUrl)
    {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();

        this.objectMapper = new ObjectMapper();
        this.deserializer = CheapJacksonDeserializer.withDefaultFactory();
    }

    /**
     * Creates a new CheapRestClient with a custom WebClient.
     *
     * @param webClient the WebClient to use
     */
    public CheapRestClientImpl(@NotNull WebClient webClient)
    {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
        this.deserializer = CheapJacksonDeserializer.withDefaultFactory();
    }

    // ========== Catalog Operations ==========

    @Override
    public @NotNull CreateCatalogResponse createCatalog(
        @NotNull CatalogDef catalogDef,
        @NotNull CatalogSpecies species,
        UUID upstream,
        java.net.URI uri)
    {
        CreateCatalogRequest request = new CreateCatalogRequest(catalogDef, species, upstream, uri);

        return webClient.post()
            .uri("/api/catalogs")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CreateCatalogResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public @NotNull CatalogListResponse listCatalogs(int page, int size)
    {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/catalogs")
                .queryParam("page", page)
                .queryParam("size", size)
                .build())
            .retrieve()
            .bodyToMono(CatalogListResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public @NotNull CatalogDef getCatalog(@NotNull UUID catalogId)
    {
        String json = webClient.get()
            .uri("/api/catalogs/{catalogId}", catalogId)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorMap(this::mapException)
            .block();

        try {
            return deserializer.deserializeCatalogDef(json);
        }
        catch (Exception e) {
            throw new CheapRestClientException("Failed to deserialize CatalogDef", e);
        }
    }

    // ========== AspectDef Operations ==========

    @Override
    public @NotNull CreateAspectDefResponse createAspectDef(
        @NotNull UUID catalogId,
        @NotNull AspectDef aspectDef)
    {
        String json;
        try {
            json = CheapJacksonSerializer.serializeAspectDef(aspectDef);
        }
        catch (Exception e) {
            throw new CheapRestClientException("Failed to serialize AspectDef", e);
        }

        return webClient.post()
            .uri("/api/catalogs/{catalogId}/aspect-defs", catalogId)
            .bodyValue(json)
            .retrieve()
            .bodyToMono(CreateAspectDefResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public @NotNull AspectDefListResponse listAspectDefs(@NotNull UUID catalogId, int page, int size)
    {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/catalogs/{catalogId}/aspect-defs")
                .queryParam("page", page)
                .queryParam("size", size)
                .build(catalogId))
            .retrieve()
            .bodyToMono(AspectDefListResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public @NotNull AspectDef getAspectDef(@NotNull UUID catalogId, @NotNull UUID aspectDefId)
    {
        String json = webClient.get()
            .uri("/api/catalogs/{catalogId}/aspect-defs/{aspectDefId}", catalogId, aspectDefId)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorMap(this::mapException)
            .block();

        try {
            return deserializer.deserializeAspectDef(json);
        }
        catch (Exception e) {
            throw new CheapRestClientException("Failed to deserialize AspectDef", e);
        }
    }

    @Override
    public @NotNull AspectDef getAspectDefByName(@NotNull UUID catalogId, @NotNull String aspectDefName)
    {
        String json = webClient.get()
            .uri("/api/catalogs/{catalogId}/aspect-defs/{aspectDefName}", catalogId, aspectDefName)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorMap(this::mapException)
            .block();

        try {
            return deserializer.deserializeAspectDef(json);
        }
        catch (Exception e) {
            throw new CheapRestClientException("Failed to deserialize AspectDef", e);
        }
    }

    // ========== Aspect Operations ==========

    @Override
    public @NotNull UpsertAspectsResponse upsertAspects(
        @NotNull UUID catalogId,
        @NotNull String aspectDefName,
        @NotNull Map<UUID, Map<String, Object>> aspects)
    {
        UpsertAspectsRequest request = new UpsertAspectsRequest(
            aspects.entrySet().stream()
                .map(entry -> new UpsertAspectsRequest.AspectData(
                    entry.getKey(),
                    entry.getValue()
                ))
                .toList()
        );

        return webClient.post()
            .uri("/api/catalogs/{catalogId}/aspects/{aspectDefName}", catalogId, aspectDefName)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(UpsertAspectsResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public @NotNull AspectQueryResponse queryAspects(
        @NotNull UUID catalogId,
        @NotNull Set<UUID> entityIds,
        @NotNull Set<String> aspectDefNames)
    {
        AspectQueryRequest request = new AspectQueryRequest(entityIds, aspectDefNames);

        return webClient.post()
            .uri("/api/catalogs/{catalogId}/aspects/query", catalogId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AspectQueryResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    // ========== Hierarchy Operations ==========

    @Override
    public @NotNull EntityListResponse getEntityList(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        int page,
        int size)
    {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/catalogs/{catalogId}/hierarchies/{hierarchyName}")
                .queryParam("page", page)
                .queryParam("size", size)
                .build(catalogId, hierarchyName))
            .retrieve()
            .bodyToMono(EntityListResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public @NotNull EntityDirectoryResponse getEntityDirectory(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName)
    {
        return webClient.get()
            .uri("/api/catalogs/{catalogId}/hierarchies/{hierarchyName}", catalogId, hierarchyName)
            .retrieve()
            .bodyToMono(EntityDirectoryResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public @NotNull EntityTreeResponse getEntityTree(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName)
    {
        return webClient.get()
            .uri("/api/catalogs/{catalogId}/hierarchies/{hierarchyName}", catalogId, hierarchyName)
            .retrieve()
            .bodyToMono(EntityTreeResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public @NotNull AspectMapResponse getAspectMap(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        int page,
        int size)
    {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/catalogs/{catalogId}/hierarchies/{hierarchyName}")
                .queryParam("page", page)
                .queryParam("size", size)
                .build(catalogId, hierarchyName))
            .retrieve()
            .bodyToMono(AspectMapResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    // ========== Error Handling ==========

    private Throwable mapException(Throwable throwable)
    {
        if (throwable instanceof WebClientResponseException ex) {
            return switch (ex.getStatusCode().value()) {
                case 400 -> new CheapRestBadRequestException(
                    "Bad request: " + ex.getMessage(),
                    ex
                );
                case 404 -> new CheapRestNotFoundException(
                    "Resource not found: " + ex.getMessage(),
                    ex
                );
                case 500, 503 -> new CheapRestServerException(
                    "Server error: " + ex.getMessage(),
                    ex
                );
                default -> new CheapRestClientException(
                    "HTTP error " + ex.getStatusCode() + ": " + ex.getMessage(),
                    ex
                );
            };
        }

        return new CheapRestClientException("Request failed: " + throwable.getMessage(), throwable);
    }
}
```

### Exception Classes

**CheapRestClientException.java:**
```java
package net.netbeing.cheap.rest.client.exception;

/**
 * Base exception for all Cheap REST client errors.
 */
public class CheapRestClientException extends RuntimeException
{
    public CheapRestClientException(String message)
    {
        super(message);
    }

    public CheapRestClientException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
```

**CheapRestNotFoundException.java:**
```java
package net.netbeing.cheap.rest.client.exception;

/**
 * Exception thrown when a requested resource is not found (HTTP 404).
 */
public class CheapRestNotFoundException extends CheapRestClientException
{
    public CheapRestNotFoundException(String message)
    {
        super(message);
    }

    public CheapRestNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
```

**CheapRestBadRequestException.java:**
```java
package net.netbeing.cheap.rest.client.exception;

/**
 * Exception thrown when a request is invalid (HTTP 400).
 */
public class CheapRestBadRequestException extends CheapRestClientException
{
    public CheapRestBadRequestException(String message)
    {
        super(message);
    }

    public CheapRestBadRequestException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
```

**CheapRestServerException.java:**
```java
package net.netbeing.cheap.rest.client.exception;

/**
 * Exception thrown when the server encounters an error (HTTP 5xx).
 */
public class CheapRestServerException extends CheapRestClientException
{
    public CheapRestServerException(String message)
    {
        super(message);
    }

    public CheapRestServerException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
```

### Validation Checklist

- [ ] CheapRestClient interface created with all methods
- [ ] CheapRestClientImpl created with WebClient implementation
- [ ] Exception classes created
- [ ] Error handling implemented for HTTP status codes
- [ ] JSON serialization/deserialization using cheap-json
- [ ] Module compiles successfully
- [ ] All public APIs documented with JavaDoc

## Phase 4: Testing

**Objective:** Create comprehensive unit tests for the client library using mocked WebClient responses.

### Test Resource Setup

**Copy test JSON files from cheap-rest:**

1. Copy test JSON files from `cheap-rest/src/test/resources/http-tests/` to `cheap-rest-client/src/test/resources/http-tests/`
2. These JSON files contain realistic response samples from the cheap-rest API
3. Use these files to mock WebClient responses in unit tests, ensuring tests match actual API behavior

### Test Structure

```
cheap-rest-client/src/test/
├── java/net/netbeing/cheap/rest/client/
│   └── CheapRestClientTest.java           # Unit tests with mocked WebClient
└── resources/
    └── http-tests/                        # JSON response samples copied from cheap-rest
        ├── catalog-create-response.json
        ├── catalog-list-response.json
        ├── aspectdef-create-response.json
        └── ... (other test JSON files)
```

### Unit Tests (CheapRestClientTest.java)

- Mock WebClient responses using JSON files from test resources
- Test successful requests for all endpoints
- Test error handling (400, 404, 500 status codes)
- Test serialization/deserialization using real JSON samples
- Test pagination parameters
- Verify proper exception mapping

**Note:** Integration tests (tests that start an actual cheap-rest server) will be implemented later in an independent module. This phase focuses only on unit tests with mocked responses.

### Validation Checklist

- [ ] Test JSON files copied from cheap-rest to cheap-rest-client
- [ ] Unit tests created for all client methods using mocked responses
- [ ] Tests use JSON files from resources for realistic mocking
- [ ] Error handling tests pass
- [ ] All tests pass with `./gradlew :cheap-rest-client:test`
- [ ] Test coverage > 80%

## Phase 5: Documentation and Examples

**Objective:** Provide comprehensive documentation and usage examples.

### JavaDoc

- [ ] All public classes documented
- [ ] All public methods documented with @param and @return tags
- [ ] Exception scenarios documented with @throws tags
- [ ] Code examples in class-level JavaDoc

### README

Create `cheap-rest-client/README.md` with:
- Module overview
- Quick start guide
- Usage examples
- Configuration options
- Error handling guidance

### Usage Example

```java
// Create client
CheapRestClient client = new CheapRestClientImpl("http://localhost:8080");

// Create a catalog
CatalogDef catalogDef = factory.createImmutableCatalogDef("MyDataCatalog");
CreateCatalogResponse catalogResponse = client.createCatalog(
    catalogDef,
    CatalogSpecies.DATA_CACHE,
    null,
    null
);
UUID catalogId = catalogResponse.catalogId();

// Create an aspect definition
AspectDef aspectDef = factory.createImmutableAspectDef("person", Map.of(
    "name", factory.createPropertyDef("name", PropertyType.String, false),
    "age", factory.createPropertyDef("age", PropertyType.Integer, false)
));
client.createAspectDef(catalogId, aspectDef);

// Upsert aspects
UUID entity1 = UUID.randomUUID();
Map<UUID, Map<String, Object>> aspects = Map.of(
    entity1, Map.of("name", "Alice", "age", 30)
);
UpsertAspectsResponse upsertResponse = client.upsertAspects(
    catalogId,
    "person",
    aspects
);

// Query aspects
AspectQueryResponse queryResponse = client.queryAspects(
    catalogId,
    Set.of(entity1),
    Set.of("person")
);
```

### Validation Checklist

- [ ] All public APIs have complete JavaDoc
- [ ] README.md created with examples
- [ ] Usage examples tested and verified
- [ ] `./gradlew :cheap-rest-client:javadoc` succeeds

## Implementation Checklist

### Phase 1: Move DTOs to cheap-json
- [ ] Create net.netbeing.cheap.json.dto package
- [ ] Move 13 DTO classes from cheap-rest to cheap-json
- [ ] Update package declarations in DTO files
- [ ] Update imports in cheap-rest controllers
- [ ] Update imports in cheap-rest services
- [ ] Update imports in cheap-rest tests
- [ ] Verify cheap-json builds
- [ ] Verify cheap-rest builds
- [ ] Verify all cheap-rest tests pass

### Phase 2: Create Module
- [ ] Create cheap-rest-client directory structure
- [ ] Create build.gradle.kts
- [ ] Update settings.gradle.kts
- [ ] Verify module builds

### Phase 3: Implement Client
- [ ] Create CheapRestClient interface
- [ ] Create CheapRestClientImpl
- [ ] Create exception classes
- [ ] Implement catalog operations
- [ ] Implement aspect def operations
- [ ] Implement aspect operations
- [ ] Implement hierarchy operations
- [ ] Implement error handling
- [ ] Add JavaDoc to all public APIs

### Phase 4: Testing
- [ ] Copy test JSON files from cheap-rest/src/test/resources/http-tests
- [ ] Create unit tests using mocked WebClient with JSON files
- [ ] Verify all tests pass
- [ ] Verify test coverage > 80%

### Phase 5: Documentation
- [ ] Complete all JavaDoc
- [ ] Create README.md
- [ ] Add usage examples
- [ ] Generate and review javadoc

## Dependencies Summary

### Gradle Version Catalog Additions

Verify these entries exist in `gradle/libs.versions.toml`:

```toml
[versions]
spring-boot = "3.5.6"

[libraries]
spring-boot-starter-webflux = { module = "org.springframework.boot:spring-boot-starter-webflux", version.ref = "spring-boot" }
```

### Module Dependency Graph

```
cheap-rest-client
├── cheap-core (api)
└── cheap-json (api)
    ├── cheap-core (api)
    ├── Jackson (implementation)
    └── Guava (implementation)

cheap-rest
├── cheap-core (api)
├── cheap-json (api)
├── cheap-db-* (implementation)
└── Spring Boot Web (implementation)
```

## Future Enhancements

1. **Async API** - Add reactive methods returning `Mono<T>` for non-blocking operations
2. **Retry Logic** - Implement configurable retry policies for transient failures
3. **Circuit Breaker** - Add Resilience4j integration for fault tolerance
4. **Connection Pooling** - Configure WebClient with custom connection pool settings
5. **Authentication** - Support for OAuth2, JWT, or API key authentication
6. **Caching** - Client-side caching for frequently accessed resources
7. **Metrics** - Expose client metrics (request count, latency, errors)
8. **Compression** - Support for request/response compression
9. **Multipart Uploads** - Support for uploading large datasets in chunks
10. **Streaming** - Support for streaming large result sets

## Open Questions

1. **Should the client support both blocking and reactive APIs?**
   - Current implementation uses WebClient blocking (.block())
   - Could expose reactive API returning Mono<T> for advanced users
   - **Decision:** Start with blocking API, add reactive later if needed

2. **Should the client handle pagination automatically?**
   - Current implementation requires manual page/size parameters
   - Could provide iterator-based API that fetches pages automatically
   - **Decision:** Manual pagination for now, auto-pagination in future enhancement

3. **How should the client handle partial failures in batch operations?**
   - UpsertAspectsResponse can return HTTP 207 (Multi-Status)
   - Client returns the response as-is for caller to inspect
   - **Decision:** Return raw response, let caller handle partial failures

4. **Should the client validate requests before sending?**
   - e.g., check batch size limits, validate UUIDs, etc.
   - **Decision:** No client-side validation, let server validate and return errors

5. **Should we provide a Spring Boot auto-configuration?**
   - Would allow `@Autowired CheapRestClient` in Spring applications
   - **Decision:** Deferred to future enhancement

## Success Criteria

- [ ] cheap-rest-client module builds successfully
- [ ] All unit tests pass with mocked WebClient responses
- [ ] Test coverage > 80%
- [ ] No dependencies on cheap-rest module (only cheap-json)
- [ ] All public APIs documented with JavaDoc
- [ ] README.md with usage examples
- [ ] Test JSON files copied and used for realistic mocking
- [ ] `./gradlew build` succeeds for entire project
- [ ] Integration tests deferred to separate module (future work)

## Timeline Estimate

- **Phase 1 (DTO Migration):** 2-3 hours
- **Phase 2 (Module Setup):** 1 hour
- **Phase 3 (Implementation):** 6-8 hours
- **Phase 4 (Testing):** 4-6 hours
- **Phase 5 (Documentation):** 2-3 hours

**Total:** 15-21 hours
