# Cheap REST Client

A Java client library for interacting with the Cheap REST API. This module provides a type-safe, fluent interface for managing catalogs, aspect definitions, aspects, and hierarchies.

## Features

- Type-safe API using Cheap core model objects
- Automatic JSON serialization/deserialization using Jackson
- Comprehensive exception handling with specific exception types
- Support for all Cheap REST API endpoints
- Non-blocking HTTP client using Spring WebClient

## Dependencies

- `cheap-core`: Core Cheap data model
- `cheap-json`: JSON serialization/deserialization
- Spring WebFlux: Reactive HTTP client

## Usage

### Creating a Client

```java
import net.netbeing.cheap.rest.client.CheapRestClient;
import net.netbeing.cheap.rest.client.CheapRestClientImpl;

// Create client with base URL
CheapRestClient client = new CheapRestClientImpl("http://localhost:8080");
```

### Working with Catalogs

```java
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.json.dto.CreateCatalogResponse;
import net.netbeing.cheap.json.dto.CatalogListResponse;

CheapFactory factory = new CheapFactory();

// Create a catalog
CatalogDef catalogDef = factory.createCatalogDef();
CreateCatalogResponse response = client.createCatalog(
    catalogDef,
    CatalogSpecies.DATA_CACHE,
    null  // no upstream
);
UUID catalogId = response.catalogId();

// List catalogs
CatalogListResponse catalogs = client.listCatalogs(0, 20);
System.out.println("Total catalogs: " + catalogs.totalElements());

// Get a specific catalog
CatalogDef retrievedCatalog = client.getCatalog(catalogId);
```

### Working with Aspect Definitions

```java
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.json.dto.CreateAspectDefResponse;
import net.netbeing.cheap.json.dto.AspectDefListResponse;

import java.util.Map;

// Create an aspect definition
AspectDef personDef = factory.createImmutableAspectDef(
    "person",
    UUID.randomUUID(),
    Map.of(
        "name", factory.createPropertyDef("name", PropertyType.String),
        "age", factory.createPropertyDef("age", PropertyType.Integer),
        "email", factory.createPropertyDef("email", PropertyType.String)
    )
);

CreateAspectDefResponse aspectDefResponse = client.createAspectDef(catalogId, personDef);
System.out.println("Created AspectDef: " + aspectDefResponse.aspectDefId());

// List aspect definitions
AspectDefListResponse aspectDefs = client.listAspectDefs(catalogId, 0, 20);

// Get aspect definition by ID
AspectDef retrievedDef = client.getAspectDef(catalogId, aspectDefResponse.aspectDefId());

// Get aspect definition by name
AspectDef retrievedByName = client.getAspectDefByName(catalogId, "person");
```

### Working with Aspects

```java
import net.netbeing.cheap.json.dto.UpsertAspectsResponse;
import net.netbeing.cheap.json.dto.AspectQueryResponse;

import java.util.Set;
import java.util.UUID;

// Upsert aspects for entities
UUID entity1 = UUID.randomUUID();
UUID entity2 = UUID.randomUUID();

Map<UUID, Map<String, Object>> aspects = Map.of(
    entity1, Map.of("name", "Alice", "age", 30, "email", "alice@example.com"),
    entity2, Map.of("name", "Bob", "age", 25, "email", "bob@example.com")
);

UpsertAspectsResponse upsertResponse = client.upsertAspects(
    catalogId,
    "person",
    aspects
);

System.out.println("Success: " + upsertResponse.successCount());
System.out.println("Failures: " + upsertResponse.failureCount());

// Query aspects
AspectQueryResponse queryResponse = client.queryAspects(
    catalogId,
    Set.of(entity1, entity2),
    Set.of("person")
);

// Access the results
Map<UUID, Map<String, Aspect>> results = queryResponse.results();
Aspect alicePersonAspect = results.get(entity1).get("person");
```

### Working with Hierarchies

```java
import net.netbeing.cheap.json.dto.EntityListResponse;
import net.netbeing.cheap.json.dto.EntityDirectoryResponse;
import net.netbeing.cheap.json.dto.EntityTreeResponse;
import net.netbeing.cheap.json.dto.AspectMapResponse;

// Get EntityList/EntitySet hierarchy contents
EntityListResponse listResponse = client.getEntityList(
    catalogId,
    "myEntityList",
    0,   // page
    20   // size
);

// Get EntityDirectory hierarchy contents
EntityDirectoryResponse dirResponse = client.getEntityDirectory(
    catalogId,
    "myEntityDirectory"
);

// Get EntityTree hierarchy contents
EntityTreeResponse treeResponse = client.getEntityTree(
    catalogId,
    "myEntityTree"
);

// Get AspectMap hierarchy contents
AspectMapResponse mapResponse = client.getAspectMap(
    catalogId,
    "myAspectMap",
    0,   // page
    20   // size
);
```

### Error Handling

The client throws specific exceptions for different error conditions:

```java
import net.netbeing.cheap.rest.client.exception.*;

try {
    CatalogDef catalog = client.getCatalog(nonExistentId);
} catch (CheapRestNotFoundException e) {
    // Resource not found (404)
    System.err.println("Catalog not found: " + e.getMessage());
} catch (CheapRestBadRequestException e) {
    // Invalid request (400)
    System.err.println("Bad request: " + e.getMessage());
} catch (CheapRestServerException e) {
    // Server error (5xx)
    System.err.println("Server error: " + e.getMessage());
} catch (CheapRestClientException e) {
    // Other client errors
    System.err.println("Client error: " + e.getMessage());
}
```

## Exception Hierarchy

```
CheapRestClientException (base)
├── CheapRestNotFoundException (404)
├── CheapRestBadRequestException (400)
└── CheapRestServerException (5xx)
```

## Configuration

### Custom WebClient

You can provide a custom configured WebClient:

```java
import org.springframework.web.reactive.function.client.WebClient;

WebClient customClient = WebClient.builder()
    .baseUrl("http://localhost:8080")
    .defaultHeader("Authorization", "Bearer token")
    .build();

CheapRestClient client = new CheapRestClientImpl(customClient);
```

## Building

This module is part of the Cheap multi-module Gradle project:

```bash
# Build only this module
./gradlew :cheap-rest-client:build

# Build all modules
./gradlew build
```

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.
