# cheap-db-sqlite

SQLite persistence layer for the Cheap data caching system.

## Overview

cheap-db-sqlite provides complete SQLite database persistence for Cheap catalogs. SQLite is ideal for development, testing, single-user desktop applications, and embedded scenarios where a lightweight, file-based database is preferred over a client-server database.

## Features

- **Complete Catalog Persistence**: Save and load entire catalogs with all contents
- **All Hierarchy Types Supported**: ENTITY_LIST, ENTITY_SET, ENTITY_DIR, ENTITY_TREE, ASPECT_MAP
- **Transaction Management**: Full ACID compliance with automatic rollback on errors
- **Zero Configuration**: No server setup required, just a file path
- **File-Based**: Entire database in a single portable file
- **Fast**: Excellent performance for single-user scenarios
- **In-Memory Mode**: Perfect for testing and temporary data

## Installation

### Gradle

```groovy
dependencies {
    implementation 'net.netbeing:cheap-core:0.1'
    implementation 'net.netbeing:cheap-db-sqlite:0.1'
    runtimeOnly 'org.xerial:sqlite-jdbc:3.45.0.0'
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>net.netbeing</groupId>
        <artifactId>cheap-core</artifactId>
        <version>0.1</version>
    </dependency>
    <dependency>
        <groupId>net.netbeing</groupId>
        <artifactId>cheap-db-sqlite</artifactId>
        <version>0.1</version>
    </dependency>
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.45.0.0</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

## Quick Start

### File-Based Database

```java
import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;
import net.netbeing.cheap.db.sqlite.CatalogDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.*;

// Configure DataSource for file-based database
SQLiteDataSource dataSource = new SQLiteDataSource();
dataSource.setUrl("jdbc:sqlite:/path/to/cheap.db");

// Create DAO
CatalogDao dao = new CatalogDao(dataSource);

// Create catalog
CheapFactory factory = new CheapFactory();
UUID catalogId = UUID.randomUUID();
Catalog catalog = factory.createCatalog(
    catalogId,
    CatalogSpecies.SINK,
    null,
    null,
    false
);

// Define aspect type
AspectDef personDef = factory.createImmutableAspectDef(
    "com.example.Person",
    UUID.randomUUID(),
    Map.of(
        "name", factory.createPropertyDef("name", PropertyType.String),
        "age", factory.createPropertyDef("age", PropertyType.Integer)
    )
);
catalog.extendAspectage(personDef);

// Create hierarchy
HierarchyDef hierarchyDef = factory.createHierarchyDef("people", HierarchyType.ENTITY_SET);
EntitySetHierarchy hierarchy = factory.createEntitySetHierarchy(catalog, hierarchyDef);
catalog.addHierarchy(hierarchy);

// Add entities and aspects
UUID entityId = UUID.randomUUID();
Entity entity = factory.createEntity(entityId);
hierarchy.add(entity);

Aspect aspect = factory.createAspect(
    personDef,
    entity,
    Map.of("name", "Alice", "age", 30)
);
catalog.getAspectMap(personDef).put(entity, aspect);

// Save to SQLite
dao.saveCatalog(catalog);

// Load from SQLite
Catalog loadedCatalog = dao.loadCatalog(catalogId);
```

### In-Memory Database

Perfect for testing and temporary data:

```java
// Configure in-memory database
SQLiteDataSource dataSource = new SQLiteDataSource();
dataSource.setUrl("jdbc:sqlite::memory:");

CatalogDao dao = new CatalogDao(dataSource);

// Use normally - data exists only in memory
// Destroyed when connection closes
```

## API Reference

### CatalogDao

Main class for catalog persistence operations.

#### Constructor

```java
public CatalogDao(DataSource dataSource)
```

Creates a new CatalogDao with the specified DataSource.

#### Methods

```java
// Save entire catalog (insert or update)
void saveCatalog(Catalog catalog) throws SQLException

// Load catalog by ID
Catalog loadCatalog(UUID catalogId) throws SQLException

// Check if catalog exists
boolean catalogExists(UUID catalogId) throws SQLException

// Delete catalog and all contents
void deleteCatalog(UUID catalogId) throws SQLException
```

## Configuration

### DataSource Configuration

#### Basic Configuration

```java
import org.sqlite.SQLiteDataSource;

SQLiteDataSource dataSource = new SQLiteDataSource();
dataSource.setUrl("jdbc:sqlite:/path/to/cheap.db");
```

#### With Configuration Options

```java
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

SQLiteConfig config = new SQLiteConfig();
config.setJournalMode(SQLiteConfig.JournalMode.WAL);  // Better concurrency
config.setCacheSize(10000);                            // Increase cache
config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);  // Faster writes

SQLiteDataSource dataSource = new SQLiteDataSource(config);
dataSource.setUrl("jdbc:sqlite:/path/to/cheap.db");
```

#### Using Spring Boot

```yaml
spring:
  datasource:
    url: jdbc:sqlite:${CHEAP_DB_PATH:/data/cheap.db}
    driver-class-name: org.sqlite.JDBC
```

### JDBC URL Formats

```java
// File in current directory
"jdbc:sqlite:cheap.db"

// Absolute path (Unix)
"jdbc:sqlite:/var/data/cheap.db"

// Absolute path (Windows)
"jdbc:sqlite:C:/data/cheap.db"

// In-memory database
"jdbc:sqlite::memory:"

// Temporary file
"jdbc:sqlite:"
```

## Database Schema

The module automatically creates the required schema on first use. The schema includes:

### Definition Tables
- `aspect_def` - Aspect definitions
- `property_def` - Property definitions
- `hierarchy_def` - Hierarchy definitions
- `catalog_def` - Catalog definitions

### Entity Tables
- `entity` - Entity records
- `catalog` - Catalog records
- `hierarchy` - Hierarchy records
- `aspect` - Aspect records

### Property Storage
- `property_value` - Property values for all types

### Hierarchy Content Tables
- `hierarchy_entity_list` - EntityList contents
- `hierarchy_entity_set` - EntitySet contents
- `hierarchy_entity_directory` - EntityDirectory contents
- `hierarchy_entity_tree_node` - EntityTree contents
- `hierarchy_aspect_map` - AspectMap contents

## Type Mapping

| Cheap PropertyType | SQLite Storage Class | Format           |
|--------------------|----------------------|------------------|
| Integer            | INTEGER              | 64-bit integer   |
| Float              | REAL                 | 8-byte float     |
| Boolean            | INTEGER              | 0 or 1           |
| String             | TEXT                 | UTF-8 string     |
| Text               | TEXT                 | UTF-8 string     |
| BigInteger         | TEXT                 | Decimal string   |
| BigDecimal         | TEXT                 | Decimal string   |
| DateTime           | TEXT                 | ISO-8601         |
| URI                | TEXT                 | URI string       |
| UUID               | TEXT                 | UUID string      |
| CLOB               | TEXT                 | UTF-8 string     |
| BLOB               | BLOB                 | Binary data      |

Note: SQLite uses dynamic typing with type affinities rather than strict types.

## Transaction Handling

All operations are transactional:

```java
try {
    dao.saveCatalog(catalog);
    // Success - changes committed
} catch (SQLException e) {
    // Failure - changes rolled back automatically
    System.err.println("Failed to save catalog: " + e.getMessage());
}
```

## Performance Optimization

### Enable WAL Mode

Write-Ahead Logging mode provides better concurrency:

```java
SQLiteConfig config = new SQLiteConfig();
config.setJournalMode(SQLiteConfig.JournalMode.WAL);

SQLiteDataSource dataSource = new SQLiteDataSource(config);
dataSource.setUrl("jdbc:sqlite:/path/to/cheap.db");
```

Benefits:
- Readers don't block writers
- Writers don't block readers
- Better performance in concurrent scenarios

### Increase Cache Size

```java
SQLiteConfig config = new SQLiteConfig();
config.setCacheSize(10000);  // Pages to cache (default: 2000)
```

### Batch Operations

Group multiple operations in a single transaction:

```java
// Efficient: All in one transaction
for (Entity entity : entities) {
    Aspect aspect = createAspect(entity);
    aspectMap.put(entity, aspect);
}
dao.saveCatalog(catalog);

// Inefficient: Multiple transactions
for (Aspect aspect : aspects) {
    catalog.saveAspect(aspect);  // Don't do this
}
```

### Vacuum Database

Reclaim unused space:

```java
try (Connection conn = dataSource.getConnection();
     Statement stmt = conn.createStatement()) {
    stmt.execute("VACUUM");
}
```

### Analyze Statistics

Update query planner statistics:

```java
try (Connection conn = dataSource.getConnection();
     Statement stmt = conn.createStatement()) {
    stmt.execute("ANALYZE");
}
```

## File Management

### Backup Database

```java
import java.nio.file.*;

// Simple file copy (no active connections required)
Path source = Paths.get("/path/to/cheap.db");
Path backup = Paths.get("/path/to/cheap_backup.db");

Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);

// Copy WAL files too if using WAL mode
Files.copy(
    Paths.get("/path/to/cheap.db-wal"),
    Paths.get("/path/to/cheap_backup.db-wal"),
    StandardCopyOption.REPLACE_EXISTING
);
```

### Check Database Size

```java
File dbFile = new File("/path/to/cheap.db");
long sizeBytes = dbFile.length();
System.out.println("Database size: " + sizeBytes + " bytes");
```

### Move Database

```java
// Close all connections first
dataSource = null;

// Then move the file
Files.move(
    Paths.get("/old/path/cheap.db"),
    Paths.get("/new/path/cheap.db"),
    StandardCopyOption.REPLACE_EXISTING
);
```

## Testing

SQLite is excellent for testing:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogDaoTest {
    private CatalogDao dao;

    @BeforeEach
    void setup() {
        // Each test gets fresh in-memory database
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite::memory:");
        dao = new CatalogDao(dataSource);
    }

    @Test
    void testSaveAndLoad() {
        // Test code here
        // Database automatically cleaned up after test
    }
}
```

## Error Handling

Common issues and solutions:

### SQLite Locked
```
Cause: Another process or thread has locked the database
Solution: Enable WAL mode, or ensure only one writer at a time
```

### File Not Found
```
Cause: Invalid file path or directory doesn't exist
Solution: Ensure directory exists before creating database
```

### Read-Only Database
```
Cause: No write permissions on database file or directory
Solution: Check file permissions, ensure directory is writable
```

## Limitations

### Single Writer

SQLite supports only one write transaction at a time:
- Multiple readers can run concurrently (with WAL mode)
- Only one writer can be active
- Writers block other writers

Solution: Use WAL mode and batch writes in transactions.

### Network Access

SQLite is not designed for network access:
- File-based, not client-server
- Network file systems can cause corruption
- Use `cheap-rest` module for network access

### Concurrency

Limited compared to client-server databases:
- Good for single-user applications
- Acceptable for low-concurrency multi-user
- Not suitable for high-concurrency scenarios

## When to Use SQLite

### Good Use Cases

- Development and testing
- Single-user desktop applications
- Mobile applications
- Embedded systems
- Prototyping and demos
- Small to medium datasets (< 100K entities)
- Applications requiring zero configuration

### Consider PostgreSQL/MariaDB For

- Multi-user web applications
- High write concurrency
- Large datasets (> 100K entities)
- Network-accessible databases
- Production enterprise applications
- Applications requiring advanced database features

## Migration to/from Other Databases

The API is identical across all database backends:

```java
// Load from SQLite
CatalogDao sqliteDao = new CatalogDao(sqliteDataSource);
Catalog catalog = sqliteDao.loadCatalog(catalogId);

// Save to PostgreSQL
CatalogDao postgresDao = new CatalogDao(postgresDataSource);
postgresDao.saveCatalog(catalog);
```

## Building

```bash
# Build module
./gradlew :cheap-db-sqlite:build

# Run tests (no external database required)
./gradlew :cheap-db-sqlite:test
```

## Related Modules

- **cheap-core** - Core data model (required dependency)
- **cheap-db-postgres** - PostgreSQL implementation
- **cheap-db-mariadb** - MariaDB implementation
- **cheap-rest** - REST API for network access

## Resources

- [SQLite Documentation](https://www.sqlite.org/docs.html)
- [SQLite JDBC Driver](https://github.com/xerial/sqlite-jdbc)
- [When to Use SQLite](https://www.sqlite.org/whentouse.html)

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.
