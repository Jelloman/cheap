# cheap-json

JSON serialization and deserialization library for the Cheap data model using Jackson.

## Overview

cheap-json provides complete JSON support for Cheap data structures, including catalogs, aspects, hierarchies, and all property types. It includes custom Jackson serializers, deserializers, and data transfer objects (DTOs) for clean JSON representation.

## Features

- **Complete JSON Support**: Serialize and deserialize all Cheap data model elements
- **Jackson Integration**: Custom serializers and deserializers for Cheap types
- **Type-Safe DTOs**: Immutable records for API requests and responses
- **Property Type Handling**: Proper JSON representation for all Cheap property types
- **Pretty Printing**: Human-readable JSON output option
- **Round-Trip Serialization**: Perfect fidelity for save/load operations

## Installation

### Gradle

```groovy
dependencies {
    implementation 'net.netbeing:cheap-core:0.1'
    implementation 'net.netbeing:cheap-json:0.1'
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>net.netbeing</groupId>
        <artifactId>cheap-core</artifactId>
        <version>0.1</version>
    </dependency>
    <dependency>
        <groupId>net.netbeing</groupId>
        <artifactId>cheap-json</artifactId>
        <version>0.1</version>
    </dependency>
</dependencies>
```

## Quick Start

### Basic Serialization

```java
import net.netbeing.cheap.json.CheapJsonMapper;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

// Get configured ObjectMapper
ObjectMapper mapper = CheapJsonMapper.getMapper();

// Create a catalog
CheapFactory factory = new CheapFactory();
Catalog catalog = factory.createCatalog(
    UUID.randomUUID(),
    CatalogSpecies.SINK,
    null,
    null,
    false
);

// Serialize to JSON
String json = mapper.writeValueAsString(catalog);
System.out.println(json);
```

### Basic Deserialization

```java
// Deserialize from JSON
String json = "{\"id\":\"550e8400-e29b-41d4-a716-446655440000\", ...}";
Catalog catalog = mapper.readValue(json, Catalog.class);

System.out.println("Loaded catalog: " + catalog.getId());
```

### Pretty Printing

```java
ObjectMapper mapper = CheapJsonMapper.getMapper();

String prettyJson = mapper.writerWithDefaultPrettyPrinter()
    .writeValueAsString(catalog);

System.out.println(prettyJson);
```

## API Reference

### CheapJsonMapper

Factory class for creating configured Jackson ObjectMapper instances.

#### Methods

```java
// Get a configured ObjectMapper with all Cheap serializers/deserializers
public static ObjectMapper getMapper()

// Get ObjectMapper configured for pretty printing
public static ObjectMapper getPrettyMapper()
```

## JSON Format

### Catalog JSON

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "species": "SINK",
  "upstream": null,
  "uri": "http://example.com/catalog",
  "readOnly": false,
  "catalogDef": {
    "hierarchyDefs": [...],
    "aspectDefs": {...}
  },
  "hierarchies": [...],
  "aspectage": {...}
}
```

### AspectDef JSON

```json
{
  "name": "com.example.Person",
  "id": "750e8400-e29b-41d4-a716-446655440000",
  "uri": "http://example.com/aspects/person",
  "version": 1,
  "properties": {
    "name": {
      "name": "name",
      "type": "STRING",
      "isReadable": true,
      "isWritable": true,
      "isNullable": false,
      "isRemovable": false,
      "isMultivalued": false
    },
    "age": {
      "name": "age",
      "type": "INTEGER",
      "isReadable": true,
      "isWritable": true,
      "isNullable": true,
      "isRemovable": false,
      "isMultivalued": false
    },
    "tags": {
      "name": "tags",
      "type": "STRING",
      "isReadable": true,
      "isWritable": true,
      "isNullable": false,
      "isRemovable": false,
      "isMultivalued": true
    }
  }
}
```

### Aspect JSON

```json
{
  "entityId": "850e8400-e29b-41d4-a716-446655440000",
  "aspectDefName": "com.example.Person",
  "properties": {
    "name": "Alice Johnson",
    "age": 30,
    "tags": ["developer", "manager"]
  }
}
```

### Hierarchy JSON

```json
{
  "name": "people",
  "type": "ENTITY_SET",
  "version": 1,
  "contents": [
    "850e8400-e29b-41d4-a716-446655440000",
    "850e8400-e29b-41d4-a716-446655440001"
  ]
}
```

## Property Type JSON Representation

| PropertyType | JSON Type | Example                                    |
|--------------|-----------|-------------------------------------------|
| Integer      | Number    | `42`                                      |
| Float        | Number    | `3.14159`                                 |
| Boolean      | Boolean   | `true`                                    |
| String       | String    | `"Hello, World!"`                         |
| Text         | String    | `"Long text..."`                          |
| BigInteger   | String    | `"123456789012345678901234567890"`        |
| BigDecimal   | String    | `"3.141592653589793238462643383279"`      |
| DateTime     | String    | `"2025-01-15T10:30:00Z"`                  |
| URI          | String    | `"http://example.com/resource"`           |
| UUID         | String    | `"550e8400-e29b-41d4-a716-446655440000"`  |
| CLOB         | String    | `"Large text content..."`                 |
| BLOB         | String    | `"base64EncodedData..."`                  |

### Multivalued Properties

Multivalued properties are represented as JSON arrays:

```json
{
  "tags": ["important", "urgent", "review"]
}
```

## Data Transfer Objects (DTOs)

The module provides DTOs for common operations:

### CreateCatalogRequest

```java
public record CreateCatalogRequest(
    CatalogDef catalogDef,
    CatalogSpecies species,
    UUID upstream,
    URI uri
) {}
```

### CreateAspectDefRequest

```java
public record CreateAspectDefRequest(
    String name,
    UUID id,
    Map<String, PropertyDef> properties
) {}
```

### UpsertAspectsRequest

```java
public record UpsertAspectsRequest(
    List<AspectData> aspects,
    boolean createEntities
) {}
```

### AspectQueryRequest

```java
public record AspectQueryRequest(
    Set<UUID> entityIds,
    Set<String> aspectDefNames
) {}
```

## Advanced Usage

### Handling Null Values

By default, null values are included in JSON:

```java
// Include null values
ObjectMapper mapper = CheapJsonMapper.getMapper();

// Exclude null values
ObjectMapper mapper = new ObjectMapper();
mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
```

### Custom Serialization

Implement custom serializers for specific needs:

```java
import com.fasterxml.jackson.databind.module.SimpleModule;

SimpleModule module = new SimpleModule();
module.addSerializer(CustomType.class, new CustomSerializer());

ObjectMapper mapper = CheapJsonMapper.getMapper();
mapper.registerModule(module);
```

### Streaming Large Catalogs

For very large catalogs, use streaming:

```java
ObjectMapper mapper = CheapJsonMapper.getMapper();

// Write to stream
try (JsonGenerator generator = mapper.getFactory()
        .createGenerator(outputStream)) {

    generator.writeStartObject();
    generator.writeStringField("id", catalog.getId().toString());
    // ... write more fields
    generator.writeEndObject();
}

// Read from stream
try (JsonParser parser = mapper.getFactory()
        .createParser(inputStream)) {

    // Parse incrementally
    while (parser.nextToken() != null) {
        // Process tokens
    }
}
```

### Validation

Use Jackson's validation integration:

```java
import jakarta.validation.constraints.*;

public record PersonRequest(
    @NotBlank String name,
    @Min(0) @Max(150) Integer age,
    @Email String email
) {}

// Validate during deserialization
ObjectMapper mapper = CheapJsonMapper.getMapper();
mapper.registerModule(new ValidationModule());
```

## Error Handling

### JsonProcessingException

```java
try {
    Catalog catalog = mapper.readValue(json, Catalog.class);
} catch (JsonProcessingException e) {
    System.err.println("Invalid JSON: " + e.getMessage());
    // Handle parse error
}
```

### JsonMappingException

```java
try {
    String json = mapper.writeValueAsString(catalog);
} catch (JsonMappingException e) {
    System.err.println("Mapping error: " + e.getMessage());
    // Handle mapping error
}
```

### Unrecognized Property

```java
// Configure to ignore unknown properties
ObjectMapper mapper = new ObjectMapper();
mapper.configure(
    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
    false
);
```

## Integration with cheap-rest

The cheap-rest module uses cheap-json automatically:

```java
// Spring Boot automatically uses the configured ObjectMapper
@RestController
public class CatalogController {

    @PostMapping("/api/catalog")
    public CreateCatalogResponse createCatalog(
        @RequestBody CreateCatalogRequest request
    ) {
        // Jackson automatically deserializes request
        // and serializes response
    }
}
```

## Integration with cheap-rest-client

The cheap-rest-client uses cheap-json for serialization:

```java
import net.netbeing.cheap.rest.client.CheapRestClient;
import net.netbeing.cheap.rest.client.CheapRestClientImpl;

CheapRestClient client = new CheapRestClientImpl("http://localhost:8080");

// Automatically serializes and deserializes using cheap-json
CatalogDef catalog = client.getCatalog(catalogId);
```

## Testing

### Test JSON Serialization

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Test
void testAspectSerialization() throws Exception {
    ObjectMapper mapper = CheapJsonMapper.getMapper();

    Aspect aspect = createTestAspect();

    // Serialize
    String json = mapper.writeValueAsString(aspect);

    // Deserialize
    Aspect deserialized = mapper.readValue(json, Aspect.class);

    // Verify
    assertEquals(aspect.getEntityId(), deserialized.getEntityId());
    assertEquals(aspect.getString("name"), deserialized.getString("name"));
}
```

### Test with JSONAssert

```java
import org.skyscreamer.jsonassert.JSONAssert;

@Test
void testExpectedJsonOutput() throws Exception {
    ObjectMapper mapper = CheapJsonMapper.getMapper();

    AspectDef aspectDef = createPersonAspectDef();
    String actualJson = mapper.writeValueAsString(aspectDef);

    String expectedJson = """
        {
          "name": "com.example.Person",
          "id": "750e8400-e29b-41d4-a716-446655440000",
          "properties": {
            "name": {
              "type": "STRING"
            }
          }
        }
        """;

    JSONAssert.assertEquals(expectedJson, actualJson, false);
}
```

## Performance Tips

### Reuse ObjectMapper

ObjectMapper is thread-safe and expensive to create:

```java
// Good: Singleton ObjectMapper
private static final ObjectMapper MAPPER = CheapJsonMapper.getMapper();

// Bad: Create new each time
ObjectMapper mapper = new ObjectMapper();  // Don't repeat
```

### Disable Features You Don't Need

```java
ObjectMapper mapper = CheapJsonMapper.getMapper();
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
```

### Use Type References for Generics

```java
// For deserializing generic types
TypeReference<List<AspectDef>> typeRef = new TypeReference<>() {};
List<AspectDef> aspects = mapper.readValue(json, typeRef);
```

## Building

```bash
# Build module
./gradlew :cheap-json:build

# Run tests
./gradlew :cheap-json:test
```

## Related Modules

- **cheap-core** - Core data model (required dependency)
- **cheap-rest** - Uses this module for REST API
- **cheap-rest-client** - Uses this module for client

## Dependencies

- Jackson Core - JSON processing
- Jackson Databind - Object mapping
- Jackson Annotations - Serialization control
- Jackson Java 8 modules - Java 8 type support

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.
