# CHEAP RAG Integration

This document provides an overview of the Java integration components for the CHEAP RAG (Retrieval-Augmented Generation) system.

## Overview

The CHEAP RAG integration consists of two complementary Java modules that bridge the Java-based CHEAP framework with the Python-based cheap-rag system:

1. **cheap-rag-client**: Java REST client for consuming cheap-rag APIs
2. **cheap-rag**: Java utilities for cheap-rag, featuring a comprehensive Java metadata extractor

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Java Application                        │
│                                                                 │
│  ┌──────────────────┐         ┌─────────────────────────────┐   │
│  │  CHEAP Framework │         │    cheap-rag-client         │   │
│  │  (cheap-core)    │         │  (REST Client Library)      │   │
│  └──────────────────┘         └─────────────────────────────┘   │
│           │                              │                      │
│           │ source code                  │ HTTP REST API        │
│           ↓                              ↓                      │
│  ┌──────────────────┐         ┌─────────────────────────────┐   │
│  │   cheap-rag      │         │                             │   │
│  │  (Java Extractor)│────────→│    cheap-rag Python API     │   │
│  └──────────────────┘   JSON  │    (FastAPI)                │   │
│                               │                             │   │
└───────────────────────────────┴─────────────────────────────┴───┘
                                           │
                                           │ Vector DB + LLM
                                           ↓
                                 ┌──────────────────────┐
                                 │  RAG System          │
                                 │  - Qdrant (vectors)  │
                                 │  - OpenAI (LLM)      │
                                 └──────────────────────┘
```

## Module 1: cheap-rag-client

### Purpose

Provides a type-safe Java client for querying the cheap-rag REST API from Java applications.

### Key Features

- **Simple API**: Clean, fluent interface for RAG queries
- **Filter Building**: `FilterBuilder` for constructing metadata filters
- **Reactive Support**: Both blocking and async (Mono) methods
- **Error Handling**: Specific exception types for different HTTP status codes
- **Snake Case Mapping**: Automatic JSON snake_case ↔ camelCase conversion

### Example Usage

```java
// Create client
CheapRagClient client = new CheapRagClientImpl("http://localhost:8000");

// Build filters
Map<String, Object> filters = FilterBuilder.create()
    .language("java")
    .type("interface")
    .module("cheap-core")
    .build();

// Query with filters
QueryRequest request = QueryRequest.builder()
    .query("What interfaces define catalog operations?")
    .topK(5)
    .filters(filters)
    .temperature(0.7)
    .build();

QueryResponse response = client.query(request);

// Access results
System.out.println(response.getAnswer());
for (CitationInfo citation : response.getCitations()) {
    System.out.println("  - " + citation.getArtifactName());
}
```

### Integration Points

- **cheap-rag Python API**: Consumes `/api/query`, `/api/index/status`, `/health` endpoints
- **Java Applications**: Can be used by any Java application needing semantic search over code metadata
- **Spring WebFlux**: Uses reactive HTTP client for non-blocking operations

## Module 2: cheap-rag

### Purpose

Provides high-quality Java metadata extraction for the cheap-rag indexing pipeline.

### Key Features

- **Comprehensive Extraction**: Classes, interfaces, enums, methods, fields
- **Better than Python**: Improved Javadoc parsing, generics, annotations over javalang
- **Configurable**: Control what to extract and visibility levels
- **ID Compatibility**: Same SHA-256 ID generation as Python implementation
- **JSON Output**: MetadataArtifact format matching Python schema exactly
- **CLI + API**: Use standalone or programmatically

### Example Usage

**Command-Line:**

```bash
# Extract public API from cheap-core
java -jar cheap-rag.jar extract cheap-core/src/main/java \
    --public-only \
    --output cheap-core-api.json
```

**Programmatic:**

```java
// Configure extractor
JavaExtractorConfig config = JavaExtractorConfig.builder()
    .extractClasses(true)
    .extractMethods(true)
    .publicOnly(true)
    .build();

// Extract artifacts
JavaExtractor extractor = new JavaExtractor(config);
List<MetadataArtifact> artifacts = extractor.extract(
    Paths.get("cheap-core/src/main/java")
);

// Write to JSON
ObjectMapper mapper = new ObjectMapper();
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
mapper.writeValue(new File("metadata.json"), artifacts);
```

**Python Integration:**

```python
from cheap_rag.extractors import extract_with_java

artifacts = extract_with_java(
    source_path="cheap-core/src/main/java",
    jar_path="cheap-rag.jar",
    public_only=True
)
```

### Integration Points

- **cheap-rag Python Pipeline**: Provides higher-quality Java extraction than javalang
- **Python Wrapper Script**: `cheap-rag/scripts/extract_with_java.py` for seamless integration
- **Metadata Database**: Outputs MetadataArtifact JSON matching Python schema

## Use Cases

### Use Case 1: Developer Documentation Assistant

A developer uses the Java client to query the CHEAP codebase:

```java
CheapRagClient client = new CheapRagClientImpl("http://localhost:8000");

// Ask about API usage
QueryResponse response = client.query(
    "How do I create a new Catalog with a PostgreSQL backend?"
);

System.out.println(response.getAnswer());
// Output: "To create a Catalog with PostgreSQL backend, use CheapFactory.createCatalog()..."

// Citations show exact source locations
for (CitationInfo citation : response.getCitations()) {
    System.out.println(citation.getSourceLocation());
    // Output: "cheap-core/src/main/java/CheapFactory.java:45"
}
```

### Use Case 2: Enhanced Java Indexing Pipeline

The Python cheap-rag pipeline uses the Java extractor for better results:

```python
# In cheap-rag Python code
if language == "java":
    # Use Java extractor for higher quality
    artifacts = extract_with_java(
        source_path=source_path,
        jar_path=config.java_extractor_jar
    )
else:
    # Use Python extractors for other languages
    artifacts = extract_with_python(source_path, language)

# Continue with embedding and indexing
for artifact in artifacts:
    embedding = embed_text(artifact.embedding_text)
    index.upsert(artifact.id, embedding, artifact)
```

### Use Case 3: IDE Integration

An IDE plugin uses both modules:

1. **Extract on Save**: When developer saves Java file, cheap-rag extracts metadata
2. **Background Indexing**: Metadata is sent to cheap-rag API for indexing
3. **Inline Help**: Developer hovers over a class, IDE queries cheap-rag-client for documentation
4. **Contextual Search**: Developer searches "how to use Hierarchy", gets RAG-powered answers

```java
// IDE Plugin code
public class CheapRagPlugin {
    private final JavaExtractor extractor = new JavaExtractor();
    private final CheapRagClient client = new CheapRagClientImpl("http://localhost:8000");

    public void onFileSave(File javaFile) {
        // Extract metadata
        List<MetadataArtifact> artifacts = extractor.extractFromFile(javaFile.toPath());

        // Send to indexing service (hypothetical endpoint)
        // indexService.indexArtifacts(artifacts);
    }

    public String getContextualHelp(String symbol) {
        QueryResponse response = client.query(
            QueryRequest.builder()
                .query("Explain " + symbol)
                .topK(3)
                .build()
        );
        return response.getAnswer();
    }
}
```

## Design Decisions

### Why Separate DTOs?

The cheap-rag-client defines its own DTOs instead of extending cheap-json because:

- **Different Domain**: RAG concepts (citations, embeddings, search metadata) are semantically different from CHEAP domain (catalogs, hierarchies, aspects)
- **Python API**: cheap-rag is Python-based with different conventions (snake_case)
- **Clean Boundaries**: Keeps cheap-json focused on core CHEAP model
- **No Coupling**: cheap-rag-client doesn't require cheap-core dependency

### Why Java Extractor?

The Python-based cheap-rag already has a Java extractor (using javalang). Why create a Java version?

- **Better Quality**: JavaParser provides more accurate AST parsing than javalang
- **Full Feature Support**: Better handling of generics, annotations, Javadoc
- **Performance**: Generally faster on large codebases
- **Maintainability**: Easier to maintain Java parser in Java than Python
- **Consistency**: Same ID generation algorithm ensures compatibility

### ID Generation Consistency

Both Python and Java extractors must produce identical IDs for the same artifacts:

**Algorithm:**
1. Build hash input: `"{language}:{type}:{qualifiedName}"`
2. Compute SHA-256 hash
3. Take first 16 hex characters
4. Format as: `{language}_{type}_{hash16}`

**Example:**
```
Input: "java:class:net.netbeing.cheap.CheapFactory"
SHA-256: a1b2c3d4e5f6g7h8...
ID: java_class_a1b2c3d4e5f6g7h8
```

This ensures:
- Python and Java extractors produce same IDs
- Incremental updates don't create duplicates
- References between artifacts remain valid

## Testing Strategy

### Unit Tests

Both modules have comprehensive unit tests:

**cheap-rag-client:**
- MockWebServer for HTTP mocking
- JSON test fixtures for request/response
- FilterBuilder API testing
- Error handling for each exception type

**cheap-rag:**
- @TempDir for isolated file tests
- ID generation consistency
- Visitor extraction logic
- JSON output format matching Python

### Integration Testing

**cheap-rag-client Integration:**
1. Start cheap-rag Python API (test mode)
2. Populate test data
3. Run client integration tests
4. Verify query results

**cheap-rag Integration:**
1. Extract from cheap-core with Java extractor
2. Extract same files with Python extractor
3. Compare IDs (should match)
4. Compare artifact counts (Java should be ≥ Python)
5. Verify JSON schema compatibility

### End-to-End Testing

Full pipeline test:
1. Extract Java code with cheap-rag
2. Index artifacts in cheap-rag Python API
3. Query via cheap-rag-client
4. Verify citations point to correct source locations

## Performance Characteristics

### cheap-rag-client

- **Latency**: Depends on cheap-rag API response time (typically 100-500ms)
- **Throughput**: WebClient connection pooling supports concurrent requests
- **Memory**: Minimal overhead, streams responses
- **Scalability**: Thread-safe, single client instance can be shared

### cheap-rag

- **Extraction Speed**: ~1000-5000 files per second (depends on file size)
- **Memory**: Processes files one at a time, low memory footprint
- **Parallelization**: Can process multiple directories in parallel
- **Output Size**: ~1-2KB JSON per artifact

## Configuration

### cheap-rag-client Configuration

```java
CheapRagClientConfig config = CheapRagClientConfig.builder()
    .baseUrl("http://localhost:8000")
    .connectTimeout(Duration.ofSeconds(5))
    .responseTimeout(Duration.ofSeconds(30))
    .maxConnections(50)
    .maxIdleTime(Duration.ofSeconds(30))
    .build();
```

### cheap-rag Configuration

```java
JavaExtractorConfig config = JavaExtractorConfig.builder()
    .extractClasses(true)
    .extractInterfaces(true)
    .extractEnums(true)
    .extractMethods(true)
    .extractFields(true)
    .publicOnly(false)
    .build();
```

## Future Enhancements

### Phase 2: Advanced Extraction

- **Method Bodies**: Extract method call graphs
- **Dependencies**: Track import/dependency relationships
- **Complexity Metrics**: Cyclomatic complexity, cognitive complexity
- **Test Coverage**: Link code to test cases

### Phase 3: Client Features

- **Streaming**: Stream large result sets
- **Batch Queries**: Multiple queries in one request
- **Caching**: Client-side result caching
- **Async Retry**: Exponential backoff for failed requests

### Phase 4: Language Expansion

- **TypeScript Extractor**: Extract TypeScript/JavaScript metadata
- **Python Extractor**: Extract Python metadata (in Java)
- **Rust Extractor**: Extract Rust metadata

## Documentation

- **cheap-rag-client**:
  - [README.md](cheap-rag-client/README.md) - API usage guide
  - [CLAUDE.md](cheap-rag-client/CLAUDE.md) - Development guide

- **cheap-rag**:
  - [README.md](cheap-rag/README.md) - CLI and API usage
  - [CLAUDE.md](cheap-rag/CLAUDE.md) - Development guide

## Dependencies

### cheap-rag-client

- Spring Boot Starter WebFlux (reactive HTTP)
- Jackson (JSON processing)
- Lombok (boilerplate reduction)
- SLF4J + Logback (logging)

### cheap-rag

- JavaParser 3.26.2 (AST parsing)
- Picocli 4.7.6 (CLI framework)
- Jackson (JSON output)
- SLF4J + Logback (logging)

## License

Apache License 2.0 - See LICENSE file for details.
