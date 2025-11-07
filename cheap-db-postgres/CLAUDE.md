# cheap-db-postgres Module

This file provides guidance to Claude Code when working with the cheap-db-postgres module.

## Module Overview

The cheap-db-postgres module provides PostgreSQL database persistence for Cheap catalogs. It implements the abstract database layer defined in cheap-core for PostgreSQL-specific functionality.

## Implementation Details

See [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for a complete overview of the catalog persistence implementation, including:
- Core components (CatalogPersistence interface, CatalogDao implementation)
- Supported features and database schema
- Transaction management
- Usage examples

## Package Structure

```
net.netbeing.cheap.db.postgres/
├── CatalogDao.java         # Main DAO implementation for PostgreSQL
├── PostgresCatalog.java    # PostgreSQL-backed Catalog implementation
└── util/                   # PostgreSQL-specific utilities
```

## Development Guidelines

### Database Connection

This module uses standard JDBC with PostgreSQL JDBC driver:

```java
import javax.sql.DataSource;
import net.netbeing.cheap.db.postgres.CatalogDao;

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

// Save to PostgreSQL
dao.saveCatalog(catalog);
```

### Transaction Management

- All persistence operations are transactional
- Failed operations trigger automatic rollback
- Use connection pooling (HikariCP recommended) for production

### Type Mapping

The module handles mapping between Cheap PropertyTypes and PostgreSQL column types:

| PropertyType | PostgreSQL Type      |
|--------------|----------------------|
| Integer      | BIGINT               |
| Float        | DOUBLE PRECISION     |
| Boolean      | BOOLEAN              |
| String       | VARCHAR(8192)        |
| Text         | TEXT                 |
| BigInteger   | TEXT                 |
| BigDecimal   | NUMERIC              |
| DateTime     | TIMESTAMP WITH TIME ZONE |
| URI          | VARCHAR(2048)        |
| UUID         | UUID                 |
| CLOB         | TEXT                 |
| BLOB         | BYTEA                |

### Database Schema

The module uses Flyway migrations for schema management. DDL files are located in `src/main/resources/db/migration/`.

Schema includes:
- Definition tables (aspect_def, property_def, hierarchy_def, catalog_def)
- Entity tables (entity, catalog, hierarchy, aspect)
- Property storage (property_value)
- Hierarchy content tables (hierarchy_entity_list, hierarchy_entity_set, etc.)

## Testing Guidelines

### Integration Tests

Integration tests can use embedded PostgreSQL or require a running PostgreSQL instance:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogDaoTest {
    private DataSource dataSource;
    private CatalogDao dao;

    @BeforeAll
    void setup() {
        // Option 1: Embedded PostgreSQL for testing
        EmbeddedPostgres pg = EmbeddedPostgres.start();
        dataSource = pg.getPostgresDatabase();

        // Option 2: Real PostgreSQL instance
        // dataSource = createTestDataSource();

        dao = new CatalogDao(dataSource);
    }
}
```

### Test Database Setup

For local testing with real PostgreSQL:

```sql
CREATE DATABASE cheap_test;
CREATE USER cheap_test WITH PASSWORD 'test_password';
GRANT ALL PRIVILEGES ON DATABASE cheap_test TO cheap_test;
```

Configure test properties in `src/test/resources/application-test.yml`.

### Test Data

- Use fixed UUIDs for reproducibility
- Clean up test data in `@AfterEach` methods
- Test all hierarchy types (ENTITY_LIST, ENTITY_SET, ENTITY_DIR, ENTITY_TREE, ASPECT_MAP)
- Test PostgreSQL-specific features (native UUID type, JSONB if used)

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
- Leverage PostgreSQL EXPLAIN ANALYZE for query optimization
- Consider using JSONB for complex property types

### Handling Schema Changes

- Create new Flyway migration in `src/main/resources/db/migration/`
- Follow naming convention: `V{version}__{description}.sql`
- Test migration on a copy of production data
- Document breaking changes

### Using PostgreSQL-Specific Features

PostgreSQL offers features not available in other databases:
- **Native UUID type**: Used for entity and catalog IDs
- **JSONB**: Consider for flexible property storage
- **Array types**: Useful for multivalued properties
- **Full-text search**: Can be added for text properties
- **Partitioning**: For large catalogs

## Dependencies

- **cheap-core** - Core Cheap interfaces and implementations
- **PostgreSQL JDBC Driver** - PostgreSQL JDBC driver
- **Flyway** - Database migration tool (test scope)
- **Embedded Postgres** - In-memory PostgreSQL for testing (test scope)

## Build Commands

```bash
# Build this module only
./gradlew :cheap-db-postgres:build

# Run tests (uses embedded PostgreSQL or requires PostgreSQL)
./gradlew :cheap-db-postgres:test

# Run with specific database
./gradlew :cheap-db-postgres:test -Dtest.db.url=jdbc:postgresql://localhost:5432/cheap_test
```

## Related Modules

- `cheap-core` - Core data model and abstract persistence layer
- `cheap-db-mariadb` - MariaDB implementation (similar structure)
- `cheap-db-sqlite` - SQLite implementation (similar structure)
- `cheap-rest` - REST API that can use this module

## PostgreSQL-Specific Notes

### Advantages of PostgreSQL

- **Native UUID type**: More efficient storage and indexing than string-based UUIDs
- **JSONB type**: Excellent for semi-structured data with indexing support
- **Advanced indexing**: GiST, GIN, BRIN indexes for specialized queries
- **Full-text search**: Built-in full-text search capabilities
- **Concurrency**: MVCC for excellent concurrent access
- **Extensibility**: Can add custom types and functions

### Performance Considerations

- Enable query planner statistics collection
- Use EXPLAIN ANALYZE for query optimization
- Configure shared_buffers appropriately (25% of RAM)
- Use connection pooling (required for production)
- Monitor with pg_stat_statements
- Regular VACUUM ANALYZE for statistics updates

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

### Backup and Recovery

PostgreSQL offers excellent backup options:
- `pg_dump` for logical backups
- `pg_basebackup` for physical backups
- Point-in-time recovery (PITR) with WAL archiving
- Continuous archiving for disaster recovery
