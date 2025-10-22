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
cheap-core/          # Core library module
cheap-db-mariadb/    # MariaDB Database module (depends on cheap-core)
cheap-db-postgres/   # PostgreSQL Database module (depends on cheap-core)
cheap-db-sqlite/     # Sqlite Database module (depends on cheap-core)
cheap-json/          # JSON module (depends on cheap-core)
cheap-rest/          # Spring Boot REST API module (depends on cheap-core, cheap-json, all cheap-db-* modules)
settings.gradle.kts  # Multi-module build configuration
gradle/
└── libs.versions.toml # Version catalog for dependencies
```

### Common Commands

```bash
# Build all modules
./gradlew build

# Run tests for all modules
./gradlew test

# Clean build all modules
./gradlew clean build

# Build specific module
./gradlew :cheap-core:build
./gradlew :cheap-db-mariadb:build
./gradlew :cheap-db-postgres:build
./gradlew :cheap-db-sqlite:build
./gradlew :cheap-json:build
./gradlew :cheap-rest:build

# Run cheap-rest with different database backends
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=postgres'
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=sqlite'
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=mariadb'
```

## Architecture

### Key Dependencies
- **Guava** - Core utilities (used across all modules)
- **Lombok** - Code generation (via Gradle plugin `io.freefair.lombok`)
- **JetBrains Annotations** - Null safety annotations
- **JUnit Jupiter** - Testing framework
- **SQLite JDBC** - SQLite database driver (cheap-db-sqlite module)
- **PostgreSQL JDBC** - PostgreSQL database driver (cheap-db-postgres module)
- **MariaDB JDBC** - MariaDB database driver (cheap-db-mariadb module)
- **Jackson Core/Databind** - JSON processing (cheap-json module)
- **Flyway** - Database migration tool (cheap-db tests)
- **Embedded Postgres** - In-memory PostgreSQL for testing (cheap-db tests)
- **Spring Boot** - Web framework and dependency injection (cheap-rest module)
  - **Spring Boot Starter Web** - REST API support
  - **Spring Boot Starter JDBC** - Database access
  - **Spring Boot Starter Validation** - Input validation
  - **Spring Boot Starter Actuator** - Health checks and monitoring
- **OpenAPI/Swagger** - API documentation and interactive UI (cheap-rest module)

### Package Organization
```
net.netbeing.cheap/
├── model/           # Core interfaces (Catalog, Hierarchy, Entity, Aspect, etc.)
├── impl/
│   ├── basic/       # Basic implementations (~30 classes)
│   └── reflect/     # Reflection-based implementations
├── util/            # Utility classes
│   └── reflect/     # Reflection utility classes
├── db/              # Database persistence functionality
│   └── postgres/    # Postgres DB implementation (cheap-db-postgres module)
│   └── sqlite/      # Sqlite DB implementation (cheap-db-sqlite module)
│   └── mariadb/     # MariaDB DB implementation (cheap-db-mariadb module)
├── json/            # JSON serialization and schemas (cheap-json module)
│   └── jackson/     # Jackson-specific implementations
└── rest/            # Spring Boot REST API (cheap-rest module)
    ├── controller/  # REST controllers
    ├── service/     # Business logic layer
    ├── dto/         # Data transfer objects
    ├── config/      # Spring configuration
    └── exception/   # Exception handlers
```

### Core Concepts

For detailed information about Cheap's core concepts (Catalogs, Hierarchies, Entities, Aspects, and Properties), see [.claude/docs/core-concepts.md](.claude/docs/core-concepts.md).

## Development Notes

- Lombok managed via Gradle plugin rather than direct dependencies
- Dependencies managed through Gradle version catalogs (`libs.*`)
- Null safety enforced with JetBrains annotations

# Code style
- ALWAYS put a newline at the end of every text file (.java, .js, .ts, .json, .sql, .md, etc.)
 
# Java code style
- Put left brackets at the end of the line, except for class and function bodies where they should go on the next line
- When using Cheap interfaces, prefer not to use methods named "unsafe*", except in unit tests that are specifically testing those unsafe* methods
- Always import used classes instead of using fully-qualified class names, except where conflicts require it.
- Never mark methods as deprecated unless explicitly directed to do so.

## Java unit testing
- Comprehensive test coverage with JUnit Jupiter
- Do not use mocks unless explicitly instructed
- When constructing Cheap elements, prefer constructors that pass in a fixed UUID for global ID.
- When testing JSON output, prefer testing the entire output
- Whenever testing expected String values, if an inline String would be longer than 10 lines, put it in a file in the test/resources directory instead.
