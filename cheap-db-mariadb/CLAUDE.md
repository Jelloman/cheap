# cheap-db-mariadb Module

This file provides guidance to Claude Code when working with the cheap-db-mariadb module.

## Module Overview

The cheap-db-mariadb module provides MariaDB database persistence for Cheap catalogs. It implements the abstract database layer defined in cheap-core for MariaDB-specific functionality.

## Implementation Details

See [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for a complete overview of the catalog persistence implementation, including:
- Core components (CatalogPersistence interface, CatalogDao implementation)
- Supported features and database schema
- Transaction management
- Usage examples

## Package Structure

```
net.netbeing.cheap.db.mariadb/
├── CatalogDao.java         # Main DAO implementation for MariaDB
├── MariadbCatalog.java     # MariaDB-backed Catalog implementation
└── util/                   # MariaDB-specific utilities
```

## Development Guidelines

### Database Connection

This module uses standard JDBC with MariaDB Connector/J:

```java
import javax.sql.DataSource;
import net.netbeing.cheap.db.mariadb.CatalogDao;

// Create DAO with DataSource
CatalogDao dao = new CatalogDao(dataSource);
```

### Using CheapFactory

Always use `CheapFactory` for object creation:

```java
import net.netbeing.cheap.impl.basic.CheapFactory;

CheapFactory factory = new CheapFactory();
Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, false);

// Add hierarchies and aspects...

// Save to MariaDB
dao.saveCatalog(catalog);
```

### Transaction Management

- All persistence operations are transactional
- Failed operations trigger automatic rollback
- Use connection pooling (HikariCP recommended) for production

### Type Mapping

The module handles mapping between Cheap PropertyTypes and MariaDB column types:

| PropertyType | MariaDB Type         |
|--------------|----------------------|
| Integer      | BIGINT               |
| Float        | DOUBLE               |
| Boolean      | BOOLEAN              |
| String       | VARCHAR(8192)        |
| Text         | TEXT                 |
| BigInteger   | TEXT                 |
| BigDecimal   | TEXT                 |
| DateTime     | VARCHAR(64)          |
| URI          | VARCHAR(2048)        |
| UUID         | CHAR(36)             |
| CLOB         | LONGTEXT             |
| BLOB         | LONGBLOB             |

### Database Schema

The module uses Flyway migrations for schema management. DDL files are located in `src/main/resources/db/migration/`.

Schema includes:
- Definition tables (aspect_def, property_def, hierarchy_def, catalog_def)
- Entity tables (entity, catalog, hierarchy, aspect)
- Property storage (property_value)
- Hierarchy content tables (hierarchy_entity_list, hierarchy_entity_set, etc.)

## Testing Guidelines

### Integration Tests

Integration tests require a running MariaDB instance:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogDaoTest {
    private DataSource dataSource;
    private CatalogDao dao;

    @BeforeAll
    void setup() {
        // Set up test database connection
        dataSource = createTestDataSource();
        dao = new CatalogDao(dataSource);
    }
}
```

### Test Database Setup

For local testing:

```sql
CREATE DATABASE cheap_test;
CREATE USER 'cheap_test'@'localhost' IDENTIFIED BY 'test_password';
GRANT ALL PRIVILEGES ON cheap_test.* TO 'cheap_test'@'localhost';
```

Configure test properties in `src/test/resources/application-test.yml`.

### Test Data

- Use fixed UUIDs for reproducibility
- Clean up test data in `@AfterEach` methods
- Test all hierarchy types (ENTITY_LIST, ENTITY_SET, ENTITY_DIR, ENTITY_TREE, ASPECT_MAP)

## Common Tasks

### Adding Support for a New Property Type

1. Update type mapping in `CatalogDao`
2. Add SQL handling for the new type
3. Update schema if column type changes needed
4. Add comprehensive unit tests
5. Document in README.md

### Optimizing Query Performance

- Use appropriate indexes (defined in DDL)
- Consider batch operations for bulk inserts
- Use connection pooling
- Monitor query performance with MariaDB slow query log

### Handling Schema Changes

- Create new Flyway migration in `src/main/resources/db/migration/`
- Follow naming convention: `V{version}__{description}.sql`
- Test migration on a copy of production data
- Document breaking changes

## Dependencies

- **cheap-core** - Core Cheap interfaces and implementations
- **MariaDB Connector/J** - MariaDB JDBC driver
- **Flyway** - Database migration tool (test scope)

## Build Commands

```bash
# Build this module only
./gradlew :cheap-db-mariadb:build

# Run tests (requires MariaDB)
./gradlew :cheap-db-mariadb:test

# Run with specific database
./gradlew :cheap-db-mariadb:test -Dtest.db.url=jdbc:mariadb://localhost:3306/cheap_test
```

## Related Modules

- `cheap-core` - Core data model and abstract persistence layer
- `cheap-db-postgres` - PostgreSQL implementation (similar structure)
- `cheap-db-sqlite` - SQLite implementation (similar structure)
- `cheap-rest` - REST API that can use this module

## MariaDB-Specific Notes

### Differences from PostgreSQL

- Uses different DDL syntax for auto-increment and data types
- Transaction isolation level defaults may differ
- JSON handling capabilities differ from PostgreSQL's native JSON type
- Use MariaDB-specific optimizations where appropriate

### Performance Considerations

- Enable query cache for read-heavy workloads
- Use InnoDB engine for ACID compliance
- Configure buffer pool size appropriately
- Monitor table statistics and optimize as needed

### Connection Pooling

Recommended HikariCP configuration:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```
