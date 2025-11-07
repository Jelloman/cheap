# cheap-json Module

This file provides guidance to Claude Code when working with the cheap-json module.

## Module Overview

The cheap-json module provides JSON serialization and deserialization for Cheap data model elements using Jackson. It includes custom serializers, deserializers, and data transfer objects (DTOs) for JSON representation of catalogs, aspects, hierarchies, and other Cheap elements.

## Package Structure

```
net.netbeing.cheap.json/
├── dto/                    # Data Transfer Objects for JSON representation
├── jackson/                # Jackson-specific serializers and deserializers
│   ├── serializer/         # Custom Jackson serializers
│   └── deserializer/       # Custom Jackson deserializers
└── schema/                 # JSON Schema definitions (if applicable)
```

## Development Guidelines

### Jackson Configuration

The module provides pre-configured Jackson `ObjectMapper` instances:

```java
import net.netbeing.cheap.json.CheapJsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

// Get configured ObjectMapper
ObjectMapper mapper = CheapJsonMapper.getMapper();

// Use for serialization/deserialization
String json = mapper.writeValueAsString(catalog);
Catalog catalog = mapper.readValue(json, Catalog.class);
```

### Custom Serializers

When adding new serializers:

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CustomSerializer extends StdSerializer<CustomType> {
    public CustomSerializer() {
        super(CustomType.class);
    }

    @Override
    public void serialize(CustomType value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        // Serialization logic
    }
}
```

Register in module or mapper configuration.

### Custom Deserializers

When adding new deserializers:

```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CustomDeserializer extends StdDeserializer<CustomType> {
    public CustomDeserializer() {
        super(CustomType.class);
    }

    @Override
    public CustomType deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        // Deserialization logic
    }
}
```

### Data Transfer Objects (DTOs)

Use DTOs for JSON representation:

```java
// DTOs are immutable records
public record CreateCatalogRequest(
    CatalogDef catalogDef,
    CatalogSpecies species,
    UUID upstream,
    URI uri
) {}

// Jackson handles these automatically
ObjectMapper mapper = CheapJsonMapper.getMapper();
String json = mapper.writeValueAsString(new CreateCatalogRequest(...));
```

## Testing Guidelines

### JSON Serialization Tests

Test complete round-trip serialization:

```java
@Test
void testCatalogSerialization() {
    // Create catalog
    Catalog catalog = factory.createCatalog(...);

    // Serialize to JSON
    ObjectMapper mapper = CheapJsonMapper.getMapper();
    String json = mapper.writeValueAsString(catalog);

    // Deserialize back
    Catalog deserialized = mapper.readValue(json, Catalog.class);

    // Verify equality
    assertEquals(catalog.getId(), deserialized.getId());
    // ... more assertions
}
```

### Testing Expected JSON Output

When testing expected JSON output:
- If JSON is longer than 10 lines, put it in `test/resources` directory
- Use consistent file naming: `{TestClassName}_{testMethodName}_expected.json`
- Test the entire JSON structure, not just parts

```java
@Test
void testPersonAspectJson() throws IOException {
    Aspect aspect = createPersonAspect();

    ObjectMapper mapper = CheapJsonMapper.getMapper();
    String actualJson = mapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(aspect);

    String expectedJson = Files.readString(
        Paths.get("src/test/resources/PersonAspectTest_expected.json")
    );

    JSONAssert.assertEquals(expectedJson, actualJson, true);
}
```

### Testing JSON Schemas

If using JSON Schema:
- Validate generated schemas against examples
- Test schema validation with valid and invalid data
- Update schemas when data model changes

## Common Tasks

### Adding JSON Support for New Property Type

1. Add type to PropertyType enum (in cheap-core)
2. Create serializer in `jackson/serializer/` if needed
3. Create deserializer in `jackson/deserializer/` if needed
4. Register serializer/deserializer in mapper configuration
5. Add comprehensive tests
6. Document JSON format in README.md

### Adding New DTO

1. Create immutable record in `dto/` package
2. Use Jackson annotations if needed (`@JsonProperty`, `@JsonAlias`, etc.)
3. Add validation annotations if applicable
4. Write serialization/deserialization tests
5. Document in README.md

### Handling Circular References

Cheap data structures can have circular references (e.g., Catalog -> Hierarchy -> Catalog):

```java
// Use @JsonIdentityInfo to handle cycles
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"
)
public class Catalog {
    // ...
}
```

Or use custom serializers that break cycles:

```java
// Serialize only IDs for references
public void serialize(Catalog value, JsonGenerator gen, SerializerProvider provider) {
    gen.writeStartObject();
    gen.writeStringField("id", value.getId().toString());
    // Don't serialize full upstream catalog, just ID
    if (value.getUpstream() != null) {
        gen.writeStringField("upstream", value.getUpstream().getId().toString());
    }
    gen.writeEndObject();
}
```

## JSON Format Conventions

### Property Names

- Use camelCase for JSON property names
- Match Java field names when possible
- Use `@JsonProperty` for different names

### UUID Representation

- Serialize UUIDs as strings in standard format: `"550e8400-e29b-41d4-a716-446655440000"`

### DateTime Representation

- Serialize ZonedDateTime as ISO-8601 strings: `"2025-01-15T10:30:00Z"`

### Null Handling

- Include null fields by default for clarity
- Use `@JsonInclude(JsonInclude.Include.NON_NULL)` to exclude nulls when appropriate

### Collection Representation

- Serialize lists as JSON arrays
- Serialize maps as JSON objects
- Handle empty collections consistently

## Dependencies

- **cheap-core** - Core Cheap interfaces and implementations
- **Jackson Core** - JSON processing core
- **Jackson Databind** - Object mapping
- **Jackson Annotations** - Annotations for controlling serialization

## Build Commands

```bash
# Build this module only
./gradlew :cheap-json:build

# Run tests
./gradlew :cheap-json:test

# Test specific class
./gradlew :cheap-json:test --tests "CatalogSerializationTest"
```

## Related Modules

- `cheap-core` - Core data model (required dependency)
- `cheap-rest` - Uses this module for REST API JSON handling
- `cheap-rest-client` - Uses this module for client JSON handling

## Jackson Configuration Notes

### Pretty Printing

For human-readable JSON:

```java
ObjectMapper mapper = CheapJsonMapper.getMapper();
String json = mapper.writerWithDefaultPrettyPrinter()
    .writeValueAsString(catalog);
```

### Handling Unknown Properties

Configure to ignore unknown properties during deserialization:

```java
ObjectMapper mapper = new ObjectMapper();
mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
```

### Java 8 Time Support

Ensure JavaTimeModule is registered:

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());
```

### Immutable Objects

For Java records and immutable classes:

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new ParameterNamesModule());
```

## Performance Considerations

### ObjectMapper Reuse

ObjectMapper is thread-safe and expensive to create:

```java
// Good: Reuse mapper
private static final ObjectMapper MAPPER = CheapJsonMapper.getMapper();

// Bad: Create new mapper for each operation
ObjectMapper mapper = new ObjectMapper();  // Don't do this repeatedly
```

### Streaming for Large Data

For large catalogs, consider streaming:

```java
ObjectMapper mapper = CheapJsonMapper.getMapper();
JsonGenerator generator = mapper.getFactory()
    .createGenerator(outputStream);

// Write incrementally
generator.writeStartObject();
// ... write fields
generator.writeEndObject();
generator.close();
```

### Custom Type Handling

Optimize serialization of frequently used types with custom serializers.
