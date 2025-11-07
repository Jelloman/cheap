UNDER CONSTRUCTION
==================
This is a work in progress, not yet ready for prime time. 

Cheap
=====

Cheap is a data caching system and metadata model. Its design is focused on flexible and performant modeling and
usage of a wide variety of data sources and sinks, and also automated schema translation and mapping.

Cheap is NOT a database. All Cheap data is held in catalogs, and all Cheap Catalogs are caches or working copies
of external data (or other Catalogs).

The best analogy for understanding Cheap is **git**. Cheap is a git-like mechanism for structured data and objects.

| Tier          | RDBMS equivalent | Filesystem equivalent                 |
|---------------|------------------|---------------------------------------|
| C - Catalog   | Database         | Volume                                |
| H - Hierarchy | Table or Index   | Directory structure, File manifest    |
| E - Entity    | Primary Key      | File, file element                    |
| A - Aspect    | Row              | File or element attributes or content |
| P - Property  | Column           | Single attribute or content atom      |

Quick Start
-----------

### Using Cheap as a Library

```java
// Add dependency: net.netbeing:cheap-core:0.1

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.*;

CheapFactory factory = new CheapFactory();
Catalog catalog = factory.createCatalog(UUID.randomUUID(), CatalogSpecies.SINK, null, null, false);

// Define your data structure
AspectDef personDef = factory.createImmutableAspectDef("Person", UUID.randomUUID(), Map.of(
    "name", factory.createPropertyDef("name", PropertyType.String),
    "age", factory.createPropertyDef("age", PropertyType.Integer)
));

// Add data
Entity person = factory.createEntity(UUID.randomUUID());
Aspect aspect = factory.createAspect(personDef, person, Map.of("name", "Alice", "age", 30));
```

See [cheap-core README](cheap-core/README.md) for complete documentation.

### Using the REST API

Run the service:
```bash
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=sqlite'
```

Access Swagger UI at http://localhost:8080/swagger-ui.html

See [cheap-rest README](cheap-rest/README.md) for API documentation and [cheap-rest-client README](cheap-rest-client/README.md) for the Java client.

### Persisting Data

Choose a database backend:
- [PostgreSQL](cheap-db-postgres/README.md) - Recommended for production
- [SQLite](cheap-db-sqlite/README.md) - Best for development and testing
- [MariaDB](cheap-db-mariadb/README.md) - Alternative production option

Purpose
-------
I'm building Cheap primarily to serve as the data layer for a featureful desktop/web application for
interacting with and managing disparate data sources. I expect it will be useful for many other purposes.


**WORK IN PROGRESS**
--------------------
Cheap is a work in progress. I'll add a roadmap soon, for now here are the broad strokes:
* Standard AspectDefs provided by Cheap, including CatalogDef and HierarchyDef
* Better testing for large properties such as Text, CLOB, BLOB, BigDecimal, BigInteger
* Streaming/chunking methods for reading and writing multivalued properties, and Text/CLOB/BLOB
  * Decide whether Text and CLOB are both needed; probably not
* Convenience Hierarchy types, such as an AspectTree that marries an EntityTree and AspectMap to
  provide tree-based access to a specific Aspect type.
* TypeScript port
* Python port
* Maybe C++ and/or Rust ports
* protobuf, capnproto and flatbuffers support (read, write, schema translation) 
* Catalog-based task management and logging for current AND past read and write jobs to the upstream/source.


Modules
-------
| Module            | Description                                                                      | Documentation                          |
|-------------------|----------------------------------------------------------------------------------|----------------------------------------|
| cheap-core        | Core Cheap interfaces and basic implementations. Minimal dependencies.            | [README](cheap-core/README.md)         |
| cheap-db-postgres | PostgreSQL database persistence for Cheap catalogs. Recommended for production.   | [README](cheap-db-postgres/README.md)  |
| cheap-db-sqlite   | SQLite database persistence. Ideal for development, testing, and single-user apps.| [README](cheap-db-sqlite/README.md)    |
| cheap-db-mariadb  | MariaDB database persistence for Cheap catalogs.                                  | [README](cheap-db-mariadb/README.md)   |
| cheap-json        | JSON serialization and deserialization using Jackson.                             | [README](cheap-json/README.md)         |
| cheap-rest        | Spring Boot REST API service with multiple database backend support.              | [README](cheap-rest/README.md)         |
| cheap-rest-client | Java client library for accessing the Cheap REST API.                             | [README](cheap-rest-client/README.md)  |

### Planned Modules
- **cheap-net** - Networking library with support for protobuf, flatbuffers, and Cap'n Proto

DESIGN
======
See [design document](DESIGN.md) for more detail.

CATALOGS
--------
* All Cheap Catalogs are considered working copies or caches.
* Every Catalog has either an upstream Catalog or an external data source (never both).
* Each Catalog has an Aspectage, which is a directory of all AspectDefs in the catalog.

HIERARCHIES
-----------
* Hierarchies come in 5 types:

| Type        | Code | Description                                                                              |
|-------------|------|------------------------------------------------------------------------------------------|
| ENTITY_LIST | EL   | an ordered list containing entity IDs, possibly duplicates                               |
| ENTITY_SET  | ES   | a non-ordered set containing entity IDs                                                  |
| ENTITY_DIR  | ED   | a map (KV pairs) mapping strings to entity IDs                                           |
| ENTITY_TREE | ET   | a map (KV pairs) mapping strings to entity IDs OR other entity tree nodes                |
| ASPECT_MAP  | AM   | an ordered map (KV pairs) mapping local or global entity IDs to aspects of a single type |

* Hierarchies in a catalog are assigned unique names within the catalog.
  * AspectMap hierarchy names are always identical to the name of the AspectDef.


ENTITIES
--------
* Entities are nothing but UUIDs. All other information about entities is found in hierarchies and aspects.

ASPECTS
-------
* An Aspect is a data record that is attached to a single Entity.
* Each Aspect is defined by a single AspectDef which defines the fields in the record.
* Aspects are always stored in an AspectMap in a Catalog, organized by AspectDef (much like RDBMS tables).
* Each Entity can have at most one Aspect of a given AspectDef.

PROPERTIES
----------
* A Property is a "field" of data, which is either a simple value or an array of simple values ("multivalued").

Identity in Cheap
-----------------

### Global IDs
* Catalogs and AspectDefs always have a global UUID.

### Versions
* CatalogDefs, HierarchyDefs and AspectDefs have an implicit hash version, based on contents.
* Catalogs and Hierarchies have an explicit, monotonically increasing integer version number.

| Element      | Global? | Owner     | Unique ID | Version Numbering |
|--------------|---------|-----------|-----------|-------------------|
| Catalog      | Yes     | -         | UUID      | Integer (manual)  |
| CatalogDef   | No      | -         | -         | Hash (implicit)   |
| HierarchyDef | No      | Catalog   | Name      | -                 |
| Hierarchy    | No      | Catalog   | Name      | Integer (manual)  |
| Entity       | Yes     | -         | UUID      | -                 |
| AspectDef    | Yes     | -         | UUID      | Hash (implicit)   |
| Aspect       | No      | Hierarchy | Entity ID | -                 |
| PropertyDef  | No      | AspectDef | Name      | -                 |
| Property     | No      | Aspect    | Name      | -                 |


