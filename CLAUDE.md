# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Cheap is a data caching system and metadata model focused on flexible and performant modeling of various data sources and sinks. It is NOT a database - all Cheap data is held in catalogs, and all Cheap Catalogs are caches.

The system follows the CHEAP acronym structure:
- **C** - Catalog: Database equivalent / Volume
- **H** - Hierarchy: Table or Index / Directory structure, File manifest  
- **E** - Entity: Primary Key / File, file element
- **A** - Aspect: Row / File or element attributes or content
- **P** - Property: Column / Single attribute or content atom

## Build System

This is a multi-module Gradle-based Java project using Java 24 with modules.

### Project Structure

```
java/
├── cheap-core/          # Core library module
├── cheap-db/            # Database module (depends on cheap-core)
├── cheap-json/          # JSON module (depends on cheap-core)
├── settings.gradle.kts  # Multi-module build configuration
└── gradle/
    └── libs.versions.toml # Version catalog for dependencies
```

### Common Commands

```bash
# Build all modules
cd java && ./gradlew build

# Run tests for all modules
cd java && ./gradlew test

# Clean build all modules
cd java && ./gradlew clean build

# Build specific module
cd java && ./gradlew :cheap-core:build
cd java && ./gradlew :cheap-db:build
cd java && ./gradlew :cheap-json:build
```

## Architecture

### Module Structure
- **Root Project**: `cheap`
- **Core Module**: `cheap-core` - Main library containing Cheap model implementation
- **Database Module**: `cheap-db` - Database-related functionality (depends on cheap-core)
- **JSON Module**: `cheap-json` - JSON serialization and schemas (depends on cheap-core)
- **Main Package**: `net.netbeing.cheap`
- **Exported Packages**:
  - `net.netbeing.cheap.model` - Core interfaces and data model
  - `net.netbeing.cheap.impl.basic` - Basic implementations
  - `net.netbeing.cheap.impl.reflect` - Reflection-based implementations
  - `net.netbeing.cheap.util` - Utility classes
  - `net.netbeing.cheap.db` - Database persistence functionality
  - `net.netbeing.cheap.json` - JSON serialization utilities

### Key Dependencies
- **Guava** - Core utilities (used across all modules)
- **Apache Commons Math3** - Mathematical operations (API dependency in cheap-core)
- **Lombok** - Code generation (via Gradle plugin `io.freefair.lombok`)
- **JetBrains Annotations** - Null safety annotations
- **JUnit Jupiter** - Testing framework
- **SQLite JDBC** - SQLite database driver (cheap-db module)
- **PostgreSQL JDBC** - PostgreSQL database driver (cheap-db module)
- **Jackson Core/Databind** - JSON processing (cheap-json module)
- **Flyway** - Database migration tool (cheap-db tests)
- **Embedded Postgres** - In-memory PostgreSQL for testing (cheap-db tests)

### Package Organization
```
net.netbeing.cheap/
├── model/           # Core interfaces (Catalog, Hierarchy, Entity, Aspect, etc.)
├── impl/
│   ├── basic/       # Basic implementations (~30 classes)
│   └── reflect/     # Reflection-based implementations
├── util/            # Utility classes
│   └── reflect/     # Reflection utility classes
├── db/              # Database persistence functionality (cheap-db module)
└── json/            # JSON serialization and schemas (cheap-json module)
    └── jackson/     # Jackson-specific implementations
```

### Core Concepts

Cheap is analogous to **git** - it's a git-like mechanism for structured data and objects. 
Cheap elements are organized into five tiers: Catalogs, Hierarchies, Entities, Aspects, and Properties (the CHEAP acronym).

#### Catalogs

Cheap Catalogs are caches or working copies. Every catalog has either an upstream catalog or an external data source (never both).
Each catalog has exactly one "species" that determines its data source relationship and caching behavior:
- `SOURCE` - Read-only cache of an external data source
- `SINK` - Read-write working copy of an external data source
- `MIRROR` - Read-only cache of another catalog (always has same def as upstream)
- `CACHE` - Write-through cache of another catalog (writes may be buffered)
- `CLONE` - Write-back working copy of another catalog with manual reads/writes
- `FORK` - Transient copy of another catalog, severed from original (intended to become a SINK via "Save As")

Each catalog contains:
- An Aspectage - directory of all AspectDefs in the catalog
- A CatalogDef - informational definition of data types contained (AspectDefs and HierarchyDefs)
- Multiple named Hierarchies for organizing entities

#### Hierarchies

Hierarchies organize entities and aspects within a catalog. Each hierarchy has a unique name within its catalog.
There are 5 hierarchy types:
- `ENTITY_LIST` (EL) - Ordered list containing entity IDs, possibly with duplicates
- `ENTITY_SET` (ES) - Possibly-ordered set containing unique entity IDs
- `ENTITY_DIR` (ED) - String-to-entity ID mapping (dictionary-like lookups)
- `ENTITY_TREE` (ET) - Tree structure with named nodes where leaves contain entity IDs
- `ASPECT_MAP` (AM) - Possibly-ordered map of entity IDs to aspects of a single type

AspectMap hierarchy names are always identical to the name of the AspectDef they contain.
Hierarchies have an explicit, monotonically increasing integer version number.

#### Entities

Entities are conceptual objects - they are nothing but IDs. All entity information is found in hierarchies and aspects.
Entities are analogous to primary keys in database terminology.

Entity IDs can be:
- **Global**: UUIDs used for cross-catalog references (most common)
- **Local**: Implementation-specific references for performance (can lazily generate UUIDs when needed)

Each entity can have multiple Aspects attached, but no more than one of each AspectDef type. 

#### Aspects

An Aspect is a data record attached to a single Entity. Each aspect:
- Is defined by a single AspectDef which specifies the record fields
- Is stored in an AspectMap hierarchy in a catalog, organized by AspectDef (similar to RDBMS tables)
- Can exist at most once per entity for a given AspectDef

AspectDefs:
- Have a full name (globally unique, using reverse domain name notation)
- Have a UUID, and optionally a URI and version number

#### Properties

A Property is a "field" of data within an Aspect. Each property:
- Has a PropertyDef belonging to a single AspectDef
- Has a short name unique within the aspect
- Is either a simple value or an array of simple values (multivalued)
- Is always immutable (multivalued properties are not modified in place)
- Is never structured/nested - complex data should use Aspects or Hierarchies

#### Identity and Versioning

**Global IDs:**
- Catalogs and AspectDefs: Always have UUID
- AspectDefs: Also have globally unique name (reverse domain notation)
- Entities: Almost always have UUID (local entities can lazily generate)
- Catalogs: Usually have URL (all elements within are URL-addressable)
- Hierarchies: No global ID, only unique name within catalog
- PropertyDefs: No global ID, only unique name within AspectDef
- Properties: No global ID, only unique name within Aspect

**Versioning:**
- CatalogDefs, HierarchyDefs, AspectDefs: Implicit hash version (like git commits)
- Catalogs and Hierarchies: Explicit monotonically increasing integer version (manual increment, not accessible via API)

## Development Notes

- Lombok managed via Gradle plugin rather than direct dependencies
- Dependencies managed through Gradle version catalogs (`libs.*`)
- Null safety enforced with JetBrains annotations

# Code style
- Always put a newline at the end of every text file (.java, .json, .sql, .md, etc.)
- Put left brackets at the end of the line, except for class and function bodies where they should go on the next line
- When using Cheap interfaces, prefer not to use methods named "unsafe*", except in unit tests that are specifically testing those unsafe* methods

## Unit testing
- Comprehensive test coverage with JUnit Jupiter
- Do not use mocks unless explicitly instructed
- When constructing collections within a unit test, use Guava Immutable* collections whenever possible. Prefer ImmutableMap.of() to Map.of(), and ImmutableList.of() to List.of().
- When constructing Cheap elements, prefer constructors that pass in a fixed UUID for global ID.
- When testing JSON output, prefer testing the entire output
- Whenever testing expected String values, if an inline String would be longer than 10 lines, put it in a file in the test/resources directory instead.
- 