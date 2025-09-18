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
```

## Architecture

### Module Structure
- **Root Project**: `cheap`
- **Core Module**: `cheap-core` - Main library containing CHEAP model implementation
- **Database Module**: `cheap-db` - Database-related functionality (depends on cheap-core)
- **Main Package**: `net.netbeing.cheap`
- **Exported Packages**:
  - `net.netbeing.cheap.model` - Core interfaces and data model
  - `net.netbeing.cheap.impl.basic` - Basic implementations 
  - `net.netbeing.cheap.impl.reflect` - Reflection-based implementations
  - `net.netbeing.cheap.util` - Utility classes

### Key Dependencies
- **Guava** - Core utilities
- **Apache Commons Math3** - Mathematical operations (API dependency)
- **Lombok** - Code generation (via Gradle plugin `io.freefair.lombok`)
- **Jetbrains Annotations** - Null safety annotations
- **JUnit Jupiter** - Testing framework

### Package Organization
```
net.netbeing.cheap/
├── model/           # Core interfaces (Catalog, Hierarchy, Entity, Aspect, etc.)
├── impl/
│   ├── basic/       # Basic implementations (~30 classes)
│   └── reflect/     # Reflection-based implementations
└── util/            # Utility classes
```

### Core Concepts

**Catalogs**: All catalogs are caches that can be:
- Root catalogs: Represent external data sources (fixed or thin)
- Mirror catalogs: Cached views of other catalogs
- Write types: Fixed (read-only), thin (write-through), or thick (buffered writes)

**Hierarchies** come in 5 types:
- `ENTITY_LIST` (EL) - Ordered list with possible duplicates
- `ENTITY_SET` (ES) - Non-ordered set of unique entity IDs  
- `ENTITY_DIR` (ED) - String-to-entity ID mapping
- `ENTITY_TREE` (ET) - String-to-entity ID OR tree node mapping
- `ASPECT_SET` (AM) - Entity ID-to-aspect mapping for single aspect type

**Fixed Hierarchies**:
- Hierarchy 0: Entity set of all global hierarchies in catalog
- Hierarchy 1: Entity set of all entities in catalog (iteration may be expensive)

## Development Notes

- Uses Java module system with `module-info.java` and `modularity.inferModulePath = true`
- Lombok managed via Gradle plugin rather than direct dependencies
- Dependencies managed through Gradle version catalogs (`libs.*`)
- Null safety enforced with JetBrains annotations

## Unit testing
- Comprehensive test coverage with JUnit Jupiter
- Do not use mocks unless explicitly instructed
- When constructing collections within a unit test, use Guava Immutable* collections whenever possible. Prefer ImmutableMap.of() to Map.of(), and ImmutableList.of() to List.of().
- When constructing Cheap elements, prefer constructors that pass in a fixed UUID for global ID.
- When testing JSON output, prefer testing the entire output
- Whenever testing expected String values, if an inline String would be longer than 10 lines, put it in a file in the test/resources directory instead.
- 