# cheap-db-sqlite Module

This file provides guidance to Claude Code when working with the cheap-db-sqlite module.

## Module Overview

The cheap-db-sqlite module provides SQLite database persistence for Cheap catalogs. It implements the abstract database layer defined in cheap-core for SQLite-specific functionality. SQLite is ideal for development, testing, and single-user deployments.

## Package Structure

```
net.netbeing.cheap.db.sqlite/
├── CatalogDao.java         # Main DAO implementation for SQLite
├── SqliteCatalog.java      # SQLite-backed Catalog implementation
└── util/                   # SQLite-specific utilities
```

## Development Guidelines

### Database Connection

This module uses standard JDBC with SQLite JDBC driver:

```java
import javax.sql.DataSource;
import net.netbeing.cheap.db.sqlite.CatalogDao;

// Create DAO with DataSource
CatalogDao dao = new CatalogDao(dataSource);
```

### File-Based Database

SQLite uses a file-based database. Configure the JDBC URL with the file path:

```java
String jdbcUrl = "jdbc:sqlite:/path/to/cheap.db";
// Or use in-memory database for testing
String inMemoryUrl = "jdbc:sqlite::memory:";
```

### Using CheapFactory

Always use `CheapFactory` for object creation:

```java
import net.netbeing.cheap.impl.basic.CheapFactory;

CheapFactory factory = new CheapFactory();
Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, false);

// Add hierarchies and aspects...

// Save to SQLite
dao.saveCatalog(catalog);
```

### Transaction Management

- All persistence operations are transactional
- Failed operations trigger automatic rollback
- SQLite supports only one write transaction at a time
- Connection pooling is less critical for SQLite (single file)

### Type Mapping

The module handles mapping between Cheap PropertyTypes and SQLite column types:

| PropertyType | SQLite Type | Storage        |
|--------------|-------------|----------------|
| Integer      | INTEGER     | 64-bit integer |
| Float        | REAL        | 8-byte float   |
| Boolean      | INTEGER     | 0 or 1         |
| String       | TEXT        | UTF-8 text     |
| Text         | TEXT        | UTF-8 text     |
| BigInteger   | TEXT        | String format  |
| BigDecimal   | TEXT        | String format  |
| DateTime     | TEXT        | ISO-8601       |
| URI          | TEXT        | String format  |
| UUID         | TEXT        | String format  |
| CLOB         | TEXT        | UTF-8 text     |
| BLOB         | BLOB        | Binary data    |

Note: SQLite has dynamic typing, but the module uses type affinities for consistency.

### Database Schema

DDL files are located in `src/main/resources/db/`.

Schema includes:
- Definition tables (aspect_def, property_def, hierarchy_def, catalog_def)
- Entity tables (entity, catalog, hierarchy, aspect)
- Property storage (property_value)
- Hierarchy content tables (hierarchy_entity_list, hierarchy_entity_set, etc.)

## Testing Guidelines

### Integration Tests

Integration tests can use in-memory SQLite databases:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogDaoTest {
    private DataSource dataSource;
    private CatalogDao dao;

    @BeforeAll
    void setup() {
        // In-memory database for testing
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite::memory:");
        dataSource = ds;

        dao = new CatalogDao(dataSource);
    }
}
```

### Test Data

- Use fixed UUIDs for reproducibility
- Each test can use its own in-memory database
- Test all hierarchy types (ENTITY_LIST, ENTITY_SET, ENTITY_DIR, ENTITY_TREE, ASPECT_MAP)
- Test SQLite-specific constraints (single writer, type conversions)

## Common Tasks

### Adding Support for a New Property Type

1. Update type mapping in `CatalogDao`
2. Add SQL handling for the new type
3. Update schema if column changes needed
4. Add comprehensive unit tests
5. Document in README.md

### Optimizing Query Performance

- Use appropriate indexes (defined in DDL)
- Enable Write-Ahead Logging (WAL) mode for better concurrency
- Use prepared statements (already done by DAO)
- Keep database file on fast storage (SSD)
- Run ANALYZE periodically

### Handling Schema Changes

- Update DDL in `src/main/resources/db/`
- For existing databases, provide migration scripts
- Consider schema versioning
- Test migration thoroughly

### SQLite-Specific Optimizations

```java
// Enable WAL mode for better concurrency
Statement stmt = connection.createStatement();
stmt.execute("PRAGMA journal_mode=WAL");

// Increase cache size
stmt.execute("PRAGMA cache_size=10000");

// Set synchronous mode
stmt.execute("PRAGMA synchronous=NORMAL");
```

## Dependencies

- **cheap-core** - Core Cheap interfaces and implementations
- **SQLite JDBC Driver** - SQLite JDBC driver

## Build Commands

```bash
# Build this module only
./gradlew :cheap-db-sqlite:build

# Run tests (no external database required)
./gradlew :cheap-db-sqlite:test

# Tests run quickly with in-memory databases
./gradlew :cheap-db-sqlite:test --info
```

## Related Modules

- `cheap-core` - Core data model and abstract persistence layer
- `cheap-db-postgres` - PostgreSQL implementation (better for production)
- `cheap-db-mariadb` - MariaDB implementation (better for production)
- `cheap-rest` - REST API that can use this module

## SQLite-Specific Notes

### Advantages of SQLite

- **Zero Configuration**: No server setup required
- **File-Based**: Single file contains entire database
- **Portable**: Database file can be copied between systems
- **Fast**: Excellent read performance, good write performance
- **Reliable**: Well-tested with comprehensive test suite
- **Embeddable**: Runs in-process with your application
- **Cross-Platform**: Works on all major platforms

### Limitations

- **Single Writer**: Only one write transaction at a time
- **Network Access**: Not designed for network access (use cheap-rest instead)
- **Concurrency**: Limited write concurrency compared to client-server databases
- **Large Deployments**: Better suited for single-user or small-scale deployments
- **Type System**: Uses type affinity rather than strict typing

### When to Use SQLite

Use SQLite for:
- Development and testing
- Single-user desktop applications
- Embedded applications
- Mobile applications
- Small-scale deployments (< 100K entities)
- Prototyping and demos

Use PostgreSQL or MariaDB for:
- Multi-user applications
- High write concurrency
- Large-scale deployments (> 100K entities)
- Network-accessible databases
- Production web applications

### Performance Tips

1. **Enable WAL mode** for better concurrency:
   ```sql
   PRAGMA journal_mode=WAL;
   ```

2. **Batch writes** in transactions:
   ```java
   // Good: One transaction for many inserts
   dao.saveCatalog(catalogWithManyAspects);

   // Bad: Many individual transactions
   for (Aspect aspect : aspects) {
       dao.saveAspect(aspect);  // Each is a transaction
   }
   ```

3. **Use indexes** for frequently queried columns

4. **Run ANALYZE** periodically:
   ```sql
   ANALYZE;
   ```

5. **Keep database file on SSD** for best performance

### File Management

```java
// Get database file size
File dbFile = new File("/path/to/cheap.db");
long sizeBytes = dbFile.length();

// Backup database (simple file copy when no connections open)
Files.copy(
    Paths.get("/path/to/cheap.db"),
    Paths.get("/path/to/cheap_backup.db"),
    StandardCopyOption.REPLACE_EXISTING
);

// Vacuum to reclaim space
Statement stmt = connection.createStatement();
stmt.execute("VACUUM");
```

### Thread Safety

SQLite itself is thread-safe with proper configuration, but:
- Only one write transaction can be active at a time
- Multiple readers can run concurrently with WAL mode
- Use connection pooling carefully (SQLite doesn't benefit from large pools)
- Consider using a single connection with serialized access for writes
