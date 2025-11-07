# cheap-core

Core library for the Cheap data caching system, providing interfaces and implementations for the CHEAP data model.

## Overview

cheap-core is the foundation module containing:
- Core interfaces for Catalogs, Hierarchies, Entities, Aspects, and Properties
- Basic implementations of all core interfaces
- Factory classes for object creation
- Abstract database persistence layer
- Reflection-based utilities

## Installation

### Gradle

```groovy
dependencies {
    implementation 'net.netbeing:cheap-core:0.1'
}
```

### Maven

```xml
<dependency>
    <groupId>net.netbeing</groupId>
    <artifactId>cheap-core</artifactId>
    <version>0.1</version>
</dependency>
```

## The CHEAP Data Model

Cheap organizes data into five tiers (the CHEAP acronym):

| Tier          | Description                   | Analogies                                     |
|---------------|-------------------------------|-----------------------------------------------|
| **C**atalog   | Top-level container           | Database / Git repository / Filesystem volume |
| **H**ierarchy | Collection of entities/aspects| Table or Index / Directory structure          |
| **E**ntity    | Unique identifier             | Primary key / File                            |
| **A**spect    | Data record for an entity     | Table row / File attributes                   |
| **P**roperty  | Single data field             | Column / Individual attribute                 |

## Quick Start

### Creating a Catalog

```java
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.*;
import java.util.UUID;

// Create factory
CheapFactory factory = new CheapFactory();

// Create catalog
UUID catalogId = UUID.randomUUID();
Catalog catalog = factory.createCatalog(
    catalogId,
    CatalogSpecies.SINK,      // Working copy of external data
    null,                      // No upstream catalog
    null,                      // No URI
    false                      // Not read-only
);
```

### Defining Aspect Types

```java
import net.netbeing.cheap.model.*;
import java.util.Map;

// Create property definitions
PropertyDef nameProp = factory.createPropertyDef("name", PropertyType.String);
PropertyDef ageProp = factory.createPropertyDef("age", PropertyType.Integer);
PropertyDef emailProp = factory.createPropertyDef("email", PropertyType.String);

// Create aspect definition
AspectDef personDef = factory.createImmutableAspectDef(
    "com.example.Person",                    // Globally unique name
    UUID.randomUUID(),                       // Global ID
    Map.of(
        "name", nameProp,
        "age", ageProp,
        "email", emailProp
    )
);

// Add to catalog
catalog.extendAspectage(personDef);
```

### Creating Hierarchies

```java
import net.netbeing.cheap.model.*;

// Create hierarchy definition
HierarchyDef hierarchyDef = factory.createHierarchyDef("people", HierarchyType.ENTITY_SET);

// Create hierarchy
EntitySetHierarchy peopleSet = factory.createEntitySetHierarchy(catalog, hierarchyDef);

// Add to catalog
catalog.addHierarchy(peopleSet);
```

### Working with Entities and Aspects

```java
import net.netbeing.cheap.model.*;
import java.util.Map;

// Create entity
UUID entityId = UUID.randomUUID();
Entity person = factory.createEntity(entityId);

// Add entity to hierarchy
peopleSet.add(person);

// Create aspect with properties
Map<String, Object> properties = Map.of(
    "name", "Alice Johnson",
    "age", 30,
    "email", "alice@example.com"
);

Aspect personAspect = factory.createAspect(personDef, person, properties);

// Get AspectMap hierarchy for this AspectDef
AspectMapHierarchy personAspects = catalog.getAspectMap(personDef);

// Add aspect
personAspects.put(person, personAspect);
```

### Querying Data

```java
// Get aspect for entity
Aspect aspect = personAspects.get(person);

// Read properties
String name = aspect.getString("name");
Long age = aspect.getInteger("age");
String email = aspect.getString("email");

System.out.println(name + " is " + age + " years old");

// Iterate over entities in set
for (Entity entity : peopleSet) {
    Aspect a = personAspects.get(entity);
    if (a != null) {
        System.out.println(a.getString("name"));
    }
}
```

## Hierarchy Types

Cheap supports five hierarchy types for organizing entities:

### EntityList (ENTITY_LIST)
Ordered list with possible duplicates:
```java
HierarchyDef listDef = factory.createHierarchyDef("queue", HierarchyType.ENTITY_LIST);
EntityListHierarchy queue = factory.createEntityListHierarchy(catalog, listDef);

queue.add(entity1);
queue.add(entity2);
queue.add(entity1);  // Duplicates allowed
```

### EntitySet (ENTITY_SET)
Set of unique entities:
```java
HierarchyDef setDef = factory.createHierarchyDef("users", HierarchyType.ENTITY_SET);
EntitySetHierarchy users = factory.createEntitySetHierarchy(catalog, setDef);

users.add(entity1);
users.add(entity2);
users.add(entity1);  // No duplicates
```

### EntityDirectory (ENTITY_DIR)
String-to-entity mapping:
```java
HierarchyDef dirDef = factory.createHierarchyDef("usersByName", HierarchyType.ENTITY_DIR);
EntityDirectoryHierarchy directory = factory.createEntityDirectoryHierarchy(catalog, dirDef);

directory.put("alice", entity1);
directory.put("bob", entity2);

Entity alice = directory.get("alice");
```

### EntityTree (ENTITY_TREE)
Tree structure with named nodes:
```java
HierarchyDef treeDef = factory.createHierarchyDef("fileSystem", HierarchyType.ENTITY_TREE);
EntityTreeHierarchy tree = factory.createEntityTreeHierarchy(catalog, treeDef);

// Tree operations for hierarchical data
```

### AspectMap (ASPECT_MAP)
Entity-to-aspect mapping (automatically created for each AspectDef):
```java
AspectMapHierarchy aspects = catalog.getAspectMap(personDef);

aspects.put(entity, personAspect);
Aspect retrieved = aspects.get(entity);
```

## Property Types

Cheap supports the following property types:

| Type       | Java Type      | Description                        |
|------------|----------------|------------------------------------|
| Integer    | Long           | 64-bit signed integer              |
| Float      | Double         | 64-bit floating-point              |
| Boolean    | Boolean        | Boolean value (true/false/null)    |
| String     | String         | String (max 8192 chars)            |
| Text       | String         | Unlimited length text              |
| BigInteger | BigInteger     | Arbitrary precision integer        |
| BigDecimal | BigDecimal     | Arbitrary precision decimal        |
| DateTime   | ZonedDateTime  | Date/time with timezone            |
| URI        | URI            | Uniform Resource Identifier        |
| UUID       | UUID           | Universally Unique Identifier      |
| CLOB       | String         | Character Large Object (streaming) |
| BLOB       | byte[]         | Binary Large Object (streaming)    |

### Multivalued Properties

Properties can be multivalued (arrays):

```java
PropertyDef tagsProp = factory.createPropertyDef(
    "tags",
    PropertyType.String,
    true  // multivalued
);

// Create aspect with multivalued property
Map<String, Object> props = Map.of(
    "name", "Project X",
    "tags", List.of("important", "urgent", "finance")
);
```

## Catalog Species

Catalogs have a "species" that determines their relationship to data sources:

| Species | Description                                       | Use Case                  |
|---------|---------------------------------------------------|---------------------------|
| SOURCE  | Read-only cache of external data source           | Data import/viewing       |
| SINK    | Read-write working copy of external data source   | Data editing/export       |
| MIRROR  | Read-only cache of another catalog                | Replication               |
| CACHE   | Write-through cache of another catalog            | Performance optimization  |
| CLONE   | Write-back working copy with manual sync          | Offline editing           |
| FORK    | Transient copy severed from original              | "Save As" operation       |

## Factory Pattern

Always use `CheapFactory` to create objects:

```java
CheapFactory factory = new CheapFactory();

// Catalogs
Catalog catalog = factory.createCatalog(id, species, upstream, uri, readOnly);

// Definitions
AspectDef aspectDef = factory.createImmutableAspectDef(name, id, properties);
HierarchyDef hierarchyDef = factory.createHierarchyDef(name, type);
PropertyDef propertyDef = factory.createPropertyDef(name, type);

// Entities
Entity entity = factory.createEntity(uuid);

// Hierarchies
EntityListHierarchy list = factory.createEntityListHierarchy(catalog, def);
EntitySetHierarchy set = factory.createEntitySetHierarchy(catalog, def);
EntityDirectoryHierarchy dir = factory.createEntityDirectoryHierarchy(catalog, def);
EntityTreeHierarchy tree = factory.createEntityTreeHierarchy(catalog, def);

// Aspects
Aspect aspect = factory.createAspect(aspectDef, entity, properties);
```

## Thread Safety

- Individual objects are generally not thread-safe unless documented otherwise
- Use external synchronization when accessing catalogs from multiple threads
- Consider using immutable aspects for read-only scenarios

## Dependencies

- Guava - Core utilities
- Lombok - Code generation
- JetBrains Annotations - Null safety

## Related Modules

- **cheap-db-postgres** - PostgreSQL persistence
- **cheap-db-sqlite** - SQLite persistence
- **cheap-db-mariadb** - MariaDB persistence
- **cheap-json** - JSON serialization
- **cheap-rest** - REST API service
- **cheap-rest-client** - REST client library

## Building

```bash
# Build module
./gradlew :cheap-core:build

# Run tests
./gradlew :cheap-core:test
```

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.
