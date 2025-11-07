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

For detailed information about these core concepts, see [.claude/docs/core-concepts.md](.claude/docs/core-concepts.md).

## Build System

This is a multi-module Gradle-based Java project using Java 24.

### Project Structure

```
cheap-core/          # Core library module
cheap-db-mariadb/    # MariaDB persistence (depends on cheap-core)
cheap-db-postgres/   # PostgreSQL persistence (depends on cheap-core)
cheap-db-sqlite/     # SQLite persistence (depends on cheap-core)
cheap-json/          # JSON serialization (depends on cheap-core)
cheap-rest/          # Spring Boot REST API (depends on cheap-core, cheap-json, all cheap-db-*)
cheap-rest-client/   # REST client library (depends on cheap-core, cheap-json)
settings.gradle.kts  # Multi-module build configuration
gradle/
└── libs.versions.toml # Version catalog for dependencies
```

Each module has its own `CLAUDE.md` and `README.md` with module-specific guidance:
- **Module CLAUDE.md files**: Development guidelines for Claude Code
- **Module README.md files**: Usage documentation for human developers

### Common Build Commands

```bash
# Build all modules
./gradlew build

# Run tests for all modules
./gradlew test

# Clean build all modules
./gradlew clean build

# Build specific module
./gradlew :cheap-core:build
./gradlew :cheap-db-postgres:build
./gradlew :cheap-rest:build

# Run cheap-rest with different database backends
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=postgres'
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=sqlite'
./gradlew :cheap-rest:bootRun --args='--spring.profiles.active=mariadb'
```

## Module Documentation

Each module contains:
- `CLAUDE.md` - Guidance for Claude Code when working in that module
- `README.md` - Usage documentation for developers using the module

### Module Overview

| Module             | Purpose                                | Documentation                              |
|--------------------|----------------------------------------|--------------------------------------------|
| **cheap-core**     | Core interfaces and implementations    | [CLAUDE.md](cheap-core/CLAUDE.md)          |
| **cheap-db-postgres** | PostgreSQL persistence              | [CLAUDE.md](cheap-db-postgres/CLAUDE.md)   |
| **cheap-db-sqlite**   | SQLite persistence                  | [CLAUDE.md](cheap-db-sqlite/CLAUDE.md)     |
| **cheap-db-mariadb**  | MariaDB persistence                 | [CLAUDE.md](cheap-db-mariadb/CLAUDE.md)    |
| **cheap-json**     | JSON serialization/deserialization     | [CLAUDE.md](cheap-json/CLAUDE.md)          |
| **cheap-rest**     | REST API service                       | [CLAUDE.md](cheap-rest/CLAUDE.md)          |
| **cheap-rest-client** | REST client library                 | [CLAUDE.md](cheap-rest-client/CLAUDE.md)   |

Always consult the relevant module's CLAUDE.md when working in that module.

## Development Guidelines

### General Principles

- Follow the guidance in module-specific CLAUDE.md files
- Use `CheapFactory` for creating Cheap objects
- Prefer existing implementations over creating new ones
- Use JetBrains annotations for null safety
- Comprehensive test coverage required

### Code Style

#### General
- ALWAYS put a newline at the end of every text file (.java, .js, .ts, .json, .sql, .md, etc.)
- Do NOT use Java modules or generate module-info.java files

#### Java Code Style
- Put left brackets at the end of the line, except for class and function bodies where they should go on the next line
- When using Cheap interfaces, prefer not to use methods named "unsafe*", except in unit tests that are specifically testing those unsafe* methods
- Always import used classes instead of using fully-qualified class names, except where conflicts require it
- Never mark methods as deprecated unless explicitly directed to do so

#### Java Unit Testing
- Comprehensive test coverage with JUnit Jupiter
- Do not use mocks unless explicitly instructed
- When constructing Cheap elements, prefer constructors that pass in a fixed UUID for global ID
- When testing JSON output, prefer testing the entire output
- Whenever testing expected String values, if an inline String would be longer than 10 lines, put it in a file in the test/resources directory instead

## Key Technologies

- **Build System**: Gradle 8.5+ with Kotlin DSL
- **Java Version**: Java 24
- **Core Libraries**: Guava, Lombok (via Gradle plugin)
- **Testing**: JUnit Jupiter
- **Annotations**: JetBrains Annotations for null safety
- **JSON**: Jackson Core/Databind (cheap-json module)
- **Databases**: PostgreSQL, SQLite, MariaDB (cheap-db-* modules)
- **Web Framework**: Spring Boot (cheap-rest module)
- **HTTP Client**: Spring WebClient (cheap-rest-client module)

## Project Documentation

- [README.md](README.md) - Project overview and module descriptions
- [DESIGN.md](DESIGN.md) - Detailed design notes on the Cheap data model
- [.claude/docs/core-concepts.md](.claude/docs/core-concepts.md) - Core concepts explained

## Working with Modules

When working on a specific module:
1. Read the module's CLAUDE.md for development guidelines
2. Refer to the module's README.md for usage patterns
3. Check related module documentation for integration points
4. Run module-specific tests: `./gradlew :module-name:test`
5. Build the specific module: `./gradlew :module-name:build`

## Common Tasks

### Adding a New Feature

1. Determine which module(s) are affected
2. Read the CLAUDE.md for each affected module
3. Follow the guidelines in those CLAUDE.md files
4. Write tests first (TDD approach)
5. Implement the feature
6. Update module documentation if needed

### Fixing a Bug

1. Identify the module containing the bug
2. Write a failing test that reproduces the bug
3. Fix the bug following module guidelines
4. Ensure all tests pass
5. Update documentation if the bug revealed unclear behavior

### Running Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :cheap-core:test

# Specific test class
./gradlew :cheap-core:test --tests "CatalogImplTest"

# With output
./gradlew test --info
```
