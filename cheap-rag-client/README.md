# cheap-rag-client

Java REST client library for the CHEAP RAG (Retrieval-Augmented Generation) API.

## Overview

The cheap-rag-client provides a type-safe, fluent interface for interacting with the CHEAP RAG system from Java applications. It supports semantic search over metadata artifacts with citation-backed LLM answers.

## Features

- **Simple API**: Clean, fluent interface for RAG queries
- **Filter Building**: Fluent FilterBuilder for metadata filtering
- **Reactive Support**: Both blocking and async (Mono) methods
- **Error Handling**: Specific exception types for different HTTP status codes
- **Snake Case Mapping**: Automatic JSON snake_case ↔ camelCase conversion
- **Configurable**: Timeouts, connection pooling, custom WebClient support

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":cheap-rag-client"))
}
```

## Quick Start

```java
// Create client
CheapRagClient client = new CheapRagClientImpl("http://localhost:8000");

// Simple query
QueryResponse response = client.query("What is CheapFactory?");
System.out.println(response.getAnswer());

// Query with filters
Map<String, Object> filters = FilterBuilder.create()
    .language("java")
    .type("class")
    .module("cheap-core")
    .build();

QueryRequest request = QueryRequest.builder()
    .query("What is CheapFactory?")
    .topK(5)
    .filters(filters)
    .temperature(0.7)
    .build();

QueryResponse response = client.query(request);
```

## API Reference

### Client Creation

```java
// Simple constructor
CheapRagClient client = new CheapRagClientImpl("http://localhost:8000");

// With configuration
CheapRagClientConfig config = CheapRagClientConfig.builder()
    .baseUrl("http://localhost:8000")
    .connectTimeout(Duration.ofSeconds(5))
    .responseTimeout(Duration.ofSeconds(30))
    .maxConnections(50)
    .build();

CheapRagClient client = new CheapRagClientImpl(config);

// With custom WebClient (advanced)
WebClient webClient = WebClient.builder()
    .baseUrl("http://localhost:8000")
    .defaultHeader("Authorization", "Bearer " + token)
    .build();

CheapRagClient client = new CheapRagClientImpl(webClient);
```

### Query Methods

```java
// Simple query
QueryResponse query(String query);

// Full control query
QueryResponse query(QueryRequest request);

// Async query
Mono<QueryResponse> queryAsync(QueryRequest request);

// Index status
IndexStatusResponse getIndexStatus();

// Health check
Map<String, Object> healthCheck();
```

### Filter Building

```java
Map<String, Object> filters = FilterBuilder.create()
    .language("java")              // Programming language
    .type("interface")             // Artifact type
    .sourceType("code")            // Source type
    .module("cheap-core")          // Module/package
    .tags("core", "api")           // Tags
    .tableName("users")            // Database table
    .columnType("VARCHAR")         // Column type
    .primaryKey(true)              // Primary key status
    .custom("nullable", false)     // Custom fields
    .build();
```

### Query Request

```java
QueryRequest request = QueryRequest.builder()
    .query("What interfaces extend Hierarchy?")
    .topK(10)                      // Number of results (1-50)
    .similarityThreshold(0.7)      // Min similarity (0.0-1.0)
    .filters(filters)              // Metadata filters
    .temperature(0.7)              // LLM temperature (0.0-2.0)
    .maxTokens(500)                // Max tokens (100-4096)
    .build();
```

### Query Response

```java
QueryResponse response = client.query(request);

// LLM answer
String answer = response.getAnswer();

// Citations
for (CitationInfo citation : response.getCitations()) {
    System.out.println(citation.getArtifactName());
    System.out.println(citation.getSourceLocation());
}

// Source artifacts
for (ArtifactSummary source : response.getSources()) {
    System.out.println(source.getName());
    System.out.println(source.getType());
}

// Metadata
SearchMetadata search = response.getSearchMetadata();
System.out.println("Found " + search.getTotalResults() + " results");
System.out.println("Avg similarity: " + search.getAvgSimilarity());

GenerationMetadata gen = response.getGenerationMetadata();
System.out.println("Model: " + gen.getModel());
System.out.println("Tokens: " + gen.getTokensUsed());

CitationMetrics metrics = response.getCitationMetrics();
System.out.println("Citation coverage: " + metrics.getCoveragePercentage() + "%");
```

## Error Handling

```java
try {
    QueryResponse response = client.query("test");
} catch (CheapRagBadRequestException e) {
    // 400 - Invalid request
    System.err.println("Bad request: " + e.getMessage());
} catch (CheapRagNotFoundException e) {
    // 404 - Resource not found
    System.err.println("Not found: " + e.getMessage());
} catch (CheapRagServerException e) {
    // 5xx - Server error
    System.err.println("Server error: " + e.getMessage());
} catch (CheapRagClientException e) {
    // Other errors
    System.err.println("Client error: " + e.getMessage());
}
```

## Async Usage

```java
// Non-blocking query
Mono<QueryResponse> responseMono = client.queryAsync(request);

// Chain reactive operations
responseMono
    .map(QueryResponse::getAnswer)
    .doOnNext(System.out::println)
    .subscribe();

// Block when needed
QueryResponse response = responseMono.block();
```

## Examples

### Finding Interfaces

```java
Map<String, Object> filters = FilterBuilder.create()
    .language("java")
    .type("interface")
    .module("cheap-core")
    .build();

QueryRequest request = QueryRequest.builder()
    .query("What interfaces define catalog operations?")
    .filters(filters)
    .topK(5)
    .build();

QueryResponse response = client.query(request);
```

### Database Schema Queries

```java
Map<String, Object> filters = FilterBuilder.create()
    .sourceType("database")
    .tableName("users")
    .build();

QueryResponse response = client.query(
    QueryRequest.builder()
        .query("What columns are in the users table?")
        .filters(filters)
        .build()
);
```

### Index Status

```java
IndexStatusResponse status = client.getIndexStatus();
System.out.println("Total artifacts: " + status.getTotalArtifacts());
System.out.println("By type: " + status.getArtifactCountsByType());
System.out.println("By language: " + status.getArtifactCountsByLanguage());
```

## Thread Safety

The client is thread-safe and reuses the WebClient instance for efficiency. A single client instance can be shared across multiple threads.

## Dependencies

- Spring WebFlux (reactive HTTP client)
- Jackson (JSON processing)
- SLF4J (logging)

## Build Commands

```bash
# Build module
./gradlew :cheap-rag-client:build

# Run tests
./gradlew :cheap-rag-client:test
```

## License

Apache License 2.0 - See LICENSE file for details.
