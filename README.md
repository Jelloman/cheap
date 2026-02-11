Cheap
=====

Cheap is a metadata model and a data caching system. Its design is focused on flexible and performant modeling and
usage of a wide variety of data sources and sinks, and also automated schema translation and mapping. It's intended
to help developers and AI agents with porting, replatforming, database migrations, and similar types of projects.

Cheap is NOT a database. All Cheap data is held in Catalogs, and all Cheap Catalogs are caches or working copies
of external data or other Catalogs.

An analogy for understanding Cheap is **git**. Cheap is a git-like mechanism for structured data and objects.

| Tier          | RDBMS equivalent | Filesystem equivalent                 |
|---------------|------------------|---------------------------------------|
| C - Catalog   | Database         | Volume, Archive file                  |
| H - Hierarchy | Table or Index   | Directory structure, File manifest    |
| E - Entity    | Unique ID, PK    | File                                  |
| A - Aspect    | Row              | File attributes or content            |
| P - Property  | Column           | Single attribute or content atom      |

Multi-Language Ports
--------------------

Cheap is implemented in Java (primary) and has been ported to TypeScript, Python, and Rust with
consistent semantics. These ports are works in progress, and are currently behind Cheap-Java
features.

| Language   | Repository |
|------------|------------|
| Java       | [cheap](https://github.com/Jelloman/cheap) (this repo) |
| TypeScript | [cheap-ts](https://github.com/Jelloman/cheap-ts) |
| Python     | [cheap-py](https://github.com/Jelloman/cheap-py) |
| Rust       | [cheap-rust](https://github.com/Jelloman/cheap-rust) |

AI Enhancement (2026)
---------------------

Cheap is being extended with an LLM-powered metadata explorer and code assistant that provides semantic search
over metadata definitions across all four languages. The AI layer uses RAG (retrieval-augmented generation) to
enable natural-language queries about schemas with grounded, citation-backed answers.

Key capabilities in progress in [cheap-rag](https://github.com/Jelloman/cheap-rag):
- **Database schema extraction** from PostgreSQL and SQLite (via SQLAlchemy)
- **Code metadata extraction** from Java source (via javalang parser)
- **Semantic search** using embeddings (sentence-transformers) and vector search (ChromaDB)
- **LLM-powered Q&A** with citations, using local models (Qwen2.5-Coder via Ollama) or Claude API

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
Cheap serves as a data layer for applications that interact with and manage disparate data sources.
The metadata model is designed to be language-portable, developer-centric, and LLM-friendly -
making it useful both as a runtime library and as a semantic schema layer for AI-powered tools.

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

Roadmap
-------
- Standard AspectDefs provided by Cheap, including CatalogDef and HierarchyDef
- Better testing for large properties such as Text, CLOB, BLOB, BigDecimal, BigInteger
- Streaming/chunking methods for reading and writing multivalued properties, and Text/CLOB/BLOB
- Convenience Hierarchy types, such as an AspectTree that marries an EntityTree and AspectMap
- protobuf, capnproto and flatbuffers support (read, write, schema translation)
- Catalog-based task management and logging for read and write jobs

DESIGN
======
See [design document](doc/DESIGN.md) for more detail.

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
