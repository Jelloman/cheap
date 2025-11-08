# Integration Test Data Resources

This directory contains sample JSON data files used by integration tests.

## Catalog Definitions

- **sample-catalog-def.json**: Basic catalog definition with properties

## Aspect Definitions

- **sample-aspect-def-person.json**: Person aspect with firstName, lastName, age, email, active
- **sample-aspect-def-address.json**: Address aspect with street, city, state, postalCode, country
- **sample-aspect-def-product.json**: Product aspect with sku, name, price, quantity, inStock

## Aspect Data

- **sample-aspects-person.json**: Sample person aspect data with 3 test records
  - Uses fixed UUIDs (00000000-0000-0000-0000-000000000001, etc.)
  - Includes John Doe, Jane Smith, Bob Johnson

## Usage in Tests

Load JSON data using `BaseRestIntegrationTest.loadJson()` or `loadJsonAs()` methods:

```java
// Load as string
String json = loadJson("sample-catalog-def.json");

// Load and parse
CatalogDef catalogDef = loadJsonAs("sample-catalog-def.json", CatalogDef.class);

// Load aspect data
Map<UUID, Map<String, Object>> aspects = objectMapper.readValue(
    loadJson("sample-aspects-person.json"),
    new TypeReference<Map<UUID, Map<String, Object>>>() {}
);
```

## Adding New Test Data

When adding new test data files:

1. Use descriptive filenames: `sample-{type}-{name}.json`
2. Use fixed UUIDs for reproducibility
3. Follow JSON formatting conventions
4. Document the file in this README
