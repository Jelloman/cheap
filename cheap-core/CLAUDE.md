# cheap-core Module

This file provides guidance to Claude Code when working with the cheap-core module.

## Module Overview

The cheap-core module is the foundation of the Cheap data caching system. It contains:
- Core interfaces for the CHEAP data model (Catalog, Hierarchy, Entity, Aspect, Property)
- Basic implementations of all core interfaces
- Reflection-based utilities for automated aspect handling
- Abstract database access layer (concrete implementations are in cheap-db-* modules)
- Factory classes for creating Cheap objects

## Core Concepts

For detailed information about the Cheap data model, see [../.claude/docs/core-concepts.md](../.claude/docs/core-concepts.md).

## Package Structure

```
net.netbeing.cheap/
├── model/           # Core interfaces (Catalog, Hierarchy, Entity, Aspect, Property, etc.)
├── impl/
│   ├── basic/       # Basic implementations (CheapFactory, CatalogImpl, AspectDefBase, etc.)
│   └── reflect/     # Reflection-based implementations
├── util/            # Utility classes
│   └── reflect/     # Reflection utility classes
└── db/              # Abstract database persistence layer
```

## Development Guidelines

### Factory Usage

Always use `CheapFactory` to create Cheap objects rather than calling constructors directly:

```java
CheapFactory factory = new CheapFactory();

// Create catalog
Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, false);

// Create aspect definition
AspectDef personDef = factory.createImmutableAspectDef(
    "com.example.Person",
    aspectDefId,
    Map.of(
        "name", factory.createPropertyDef("name", PropertyType.String),
        "age", factory.createPropertyDef("age", PropertyType.Integer)
    )
);

// Create hierarchy
HierarchyDef hierarchyDef = factory.createHierarchyDef("people", HierarchyType.ENTITY_SET);
EntitySetHierarchy hierarchy = factory.createEntitySetHierarchy(catalog, hierarchyDef);
```

### Immutability

- Individual Property objects are always immutable
- Aspects can be mutable or immutable depending on implementation
- Use builder patterns when constructing complex objects

### Null Safety

- Use JetBrains annotations (`@Nullable`, `@NotNull`) for null safety
- Methods should explicitly document null handling behavior

### Unsafe Methods

- Avoid using methods named `unsafe*` in production code or unit tests, except when explicitly testing the unsafe methods themselves
- These methods bypass safety checks, and are meant to be used for improved performance when the safety can be guaranteed by the caller

## Testing Guidelines

### Unit Test Structure

- Comprehensive test coverage required for all public methods
- Use JUnit Jupiter for all tests
- Do not use mocks unless explicitly instructed
- Prefer actual object creation over mocking

### Test Data Creation

When constructing Cheap elements in tests:
- Pass fixed UUIDs for global IDs to ensure reproducibility
- Use descriptive names that indicate test purpose
- Construct Cheap elements with CheapFactory instead of calling constructors directly

Example:
```java
UUID testCatalogId = UUID.fromString("00000000-0000-0000-0000-000000000001");
UUID testEntityId = UUID.fromString("00000000-0000-0000-0000-000000000002");

Catalog catalog = factory.createCatalog(
    testCatalogId,
    CatalogSpecies.SINK,
    null,
    URI.create("test://catalog"),
    false
);
```

### Testing String Values

When testing expected String values:
- If inline String would be longer than 10 lines, put it in `test/resources` directory
- Use consistent file naming: `{TestClassName}_{testMethodName}_expected.txt`

## Database Persistence

The `db/` package provides an abstract layer for database persistence:
- `AbstractCheapDao` - Base class for DAO implementations
- `CheapJdbcAdapter` - JDBC-specific database adapter interface
- `JdbcCatalogBase` - Base class for JDBC-backed catalogs
- `AspectTableMapping` - Maps AspectDefs to database table structures

Concrete implementations are in:
- `cheap-db-postgres` - PostgreSQL implementation
- `cheap-db-sqlite` - SQLite implementation
- `cheap-db-mariadb` - MariaDB implementation

## Common Tasks

### Adding a New Property Type

1. Add enum value to `PropertyType` enum
2. Update `PropertyDef` interface and implementations
3. Add type conversion logic in property implementation classes
4. Update database adapters in cheap-db-* modules
5. Add JSON serialization support in cheap-json module
6. Write comprehensive unit tests

### Adding a New Hierarchy Type

1. Add enum value to `HierarchyType` enum
2. Create interface in `model/` package
3. Create implementation in `impl/basic/` package
4. Add factory methods to `CheapFactory`
5. Update database persistence in cheap-db-* modules
6. Write comprehensive unit tests

### Extending Aspect Functionality

- Prefer composition over inheritance
- Use builder patterns for complex aspect construction
- Consider reflection-based implementations for automated property handling
- Ensure thread safety if aspects will be shared across threads

## Dependencies

- **Guava** - Core utilities (ImmutableMap, ImmutableSet, etc.)
- **Lombok** - Code generation (via Gradle plugin)
- **JetBrains Annotations** - Null safety annotations
- **JUnit Jupiter** - Testing framework

## Related Modules

- `cheap-db-postgres` - PostgreSQL persistence implementation
- `cheap-db-sqlite` - SQLite persistence implementation
- `cheap-db-mariadb` - MariaDB persistence implementation
- `cheap-json` - JSON serialization/deserialization
- `cheap-rest` - REST API service
- `cheap-rest-client` - REST client library

## Build Commands

```bash
# Build this module only
./gradlew :cheap-core:build

# Run tests
./gradlew :cheap-core:test

# Clean build
./gradlew :cheap-core:clean :cheap-core:build
```
