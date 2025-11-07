# cheap-db-postgres

PostgreSQL persistence layer for the Cheap data caching system.

## Overview

cheap-db-postgres provides complete PostgreSQL database persistence for Cheap catalogs, enabling you to save and load entire catalog structures including hierarchies, entities, aspects, and properties. PostgreSQL is the recommended database backend for production deployments.

## Features

- **Complete Catalog Persistence**: Save and load entire catalogs with all contents
- **All Hierarchy Types Supported**: ENTITY_LIST, ENTITY_SET, ENTITY_DIR, ENTITY_TREE, ASPECT_MAP
- **Transaction Management**: Full ACID compliance with automatic rollback on errors
- **Type-Safe Property Storage**: Proper handling of all Cheap property types
- **Native PostgreSQL Types**: Uses UUID, TIMESTAMP WITH TIME ZONE, and other native types
- **Schema Management**: Automatic schema creation and migration via Flyway
- **Connection Pooling Support**: Works with HikariCP and other pooling libraries
- **Advanced Features**: Support for JSONB, full-text search, and advanced indexing

## Installation

### Gradle

```groovy
dependencies {
    implementation 'net.netbeing:cheap-core:0.1'
    implementation 'net.netbeing:cheap-db-postgres:0.1'
    runtimeOnly 'org.postgresql:postgresql:42.7.0'
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
        <artifactId>cheap-db-postgres</artifactId>
        <version>0.1</version>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.0</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

## Quick Start

### Database Setup

Create a PostgreSQL database and user:

```sql
CREATE DATABASE cheap;
CREATE USER cheap_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE cheap TO cheap_user;
```

### Basic Usage

```java
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.netbeing.cheap.db.postgres.CatalogDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.*;

// Configure DataSource
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:postgresql://localhost:5432/cheap");
config.setUsername("cheap_user");
config.setPassword("your_password");
DataSource dataSource = new HikariDataSource(config);

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
        "age", factory.createPropertyDef("age", PropertyType.Integer),
        "email", factory.createPropertyDef("email", PropertyType.String)
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
    Map.of("name", "Alice", "age", 30, "email", "alice@example.com")
);
catalog.getAspectMap(personDef).put(entity, aspect);

// Save to PostgreSQL
dao.saveCatalog(catalog);

// Load from PostgreSQL
Catalog loadedCatalog = dao.loadCatalog(catalogId);
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

#### Using HikariCP (Recommended)

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:postgresql://localhost:5432/cheap");
config.setUsername("cheap_user");
config.setPassword("your_password");
config.setMaximumPoolSize(10);
config.setMinimumIdle(5);
config.setConnectionTimeout(20000);
config.setIdleTimeout(300000);
config.setMaxLifetime(1200000);

DataSource dataSource = new HikariDataSource(config);
```

#### Using Spring Boot

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cheap
    username: cheap_user
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```

### Connection URL Options

Common PostgreSQL JDBC URL options:

```
jdbc:postgresql://localhost:5432/cheap?
  ssl=true&
  sslmode=require&
  ApplicationName=cheap-app
```

## Database Schema

The module automatically creates the required schema on first use. The schema includes:

### Definition Tables
- `aspect_def` - Aspect definitions
- `property_def` - Property definitions
- `hierarchy_def` - Hierarchy definitions
- `catalog_def` - Catalog definitions

### Entity Tables
- `entity` - Entity records (uses native UUID type)
- `catalog` - Catalog records (uses native UUID type)
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

| Cheap PropertyType | PostgreSQL Type           | Notes                       |
|--------------------|---------------------------|-----------------------------|
| Integer            | BIGINT                    | 64-bit signed integer       |
| Float              | DOUBLE PRECISION          | 64-bit floating point       |
| Boolean            | BOOLEAN                   | TRUE/FALSE/NULL             |
| String             | VARCHAR(8192)             | Max 8192 characters         |
| Text               | TEXT                      | Unlimited length            |
| BigInteger         | TEXT                      | Stored as string            |
| BigDecimal         | NUMERIC                   | Arbitrary precision decimal |
| DateTime           | TIMESTAMP WITH TIME ZONE  | ISO-8601 with timezone      |
| URI                | VARCHAR(2048)             | URI string                  |
| UUID               | UUID                      | Native PostgreSQL UUID      |
| CLOB               | TEXT                      | Character large object      |
| BLOB               | BYTEA                     | Binary large object         |

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

## Performance Considerations

### Indexing

The schema includes indexes on:
- Primary keys (all tables) using native UUID type
- Foreign keys (referential integrity)
- Frequently queried columns (catalog_id, entity_id, aspect_def_id)

### Batch Operations

For bulk inserts, consider using batch operations:

```java
// Save multiple aspects efficiently
for (Entity entity : entities) {
    Aspect aspect = createAspect(entity);
    aspectMap.put(entity, aspect);
}
dao.saveCatalog(catalog);  // Single transaction
```

### Connection Pooling

Always use connection pooling in production:
- Reduces connection overhead
- Limits concurrent connections
- Improves resource utilization
- Required for good PostgreSQL performance

### PostgreSQL Optimization

```sql
-- Analyze tables after bulk operations
ANALYZE;

-- Update statistics
VACUUM ANALYZE;

-- Check query performance
EXPLAIN ANALYZE SELECT ...;
```

## Advanced Features

### Using JSONB for Properties

PostgreSQL's JSONB type can be used for flexible property storage:

```sql
-- Add JSONB column for additional metadata
ALTER TABLE aspect ADD COLUMN metadata JSONB;

-- Index JSONB data
CREATE INDEX idx_aspect_metadata ON aspect USING GIN (metadata);
```

### Full-Text Search

Add full-text search for text properties:

```sql
-- Add tsvector column
ALTER TABLE property_value ADD COLUMN text_search tsvector;

-- Create GIN index
CREATE INDEX idx_property_value_text_search ON property_value USING GIN (text_search);

-- Update tsvector
UPDATE property_value SET text_search = to_tsvector('english', string_value)
WHERE string_value IS NOT NULL;
```

## Error Handling

Common exceptions and solutions:

### SQLException - Connection Failed
```
Cause: Cannot connect to PostgreSQL server
Solution: Check database is running and connection parameters are correct
```

### SQLException - Duplicate Key
```
Cause: Attempting to save catalog with existing ID
Solution: Use catalogExists() before saving, or use update operation
```

### SQLException - Foreign Key Constraint
```
Cause: Referenced entity or aspect definition doesn't exist
Solution: Ensure all definitions are saved before saving aspects
```

## Migration from Other Databases

The API is identical across all database backends. To migrate:

1. Export data from old database
2. Configure PostgreSQL DataSource
3. Import data using the same code

```java
// Load from SQLite
CatalogDao sqliteDao = new CatalogDao(sqliteDataSource);
Catalog catalog = sqliteDao.loadCatalog(catalogId);

// Save to PostgreSQL
CatalogDao postgresDao = new CatalogDao(postgresDataSource);
postgresDao.saveCatalog(catalog);
```

## Monitoring and Maintenance

### Query Performance

```sql
-- Enable pg_stat_statements
CREATE EXTENSION pg_stat_statements;

-- View slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### Database Size

```sql
-- Check database size
SELECT pg_size_pretty(pg_database_size('cheap'));

-- Check table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Backup

```bash
# Backup database
pg_dump -U cheap_user -d cheap -F c -f cheap_backup.dump

# Restore database
pg_restore -U cheap_user -d cheap cheap_backup.dump

# Point-in-time recovery (requires WAL archiving)
# Configure in postgresql.conf:
# wal_level = replica
# archive_mode = on
# archive_command = 'cp %p /path/to/archive/%f'
```

## Troubleshooting

### Schema Not Created

If schema isn't auto-created:
1. Check user has CREATE TABLE privileges
2. Check Flyway is on classpath
3. Manually run DDL from `src/main/resources/db/migration/`

### Slow Queries

If queries are slow:
1. Check indexes exist: `\di` in psql
2. Analyze query execution: `EXPLAIN ANALYZE SELECT ...`
3. Update statistics: `VACUUM ANALYZE`
4. Increase shared_buffers in postgresql.conf

### Connection Pool Exhaustion

If connection pool is exhausted:
1. Increase pool size in configuration
2. Check for connection leaks (unclosed resources)
3. Reduce connection timeout
4. Monitor connection usage with `pg_stat_activity`

## Building

```bash
# Build module
./gradlew :cheap-db-postgres:build

# Run tests (uses embedded PostgreSQL)
./gradlew :cheap-db-postgres:test
```

## Related Modules

- **cheap-core** - Core data model (required dependency)
- **cheap-db-mariadb** - MariaDB implementation
- **cheap-db-sqlite** - SQLite implementation
- **cheap-rest** - REST API that can use this module

## Why PostgreSQL?

PostgreSQL is the recommended database for production Cheap deployments:
- **Robust**: Battle-tested with excellent reliability
- **Performant**: Excellent query optimization and concurrent access
- **Feature-rich**: Native UUID, JSONB, full-text search, advanced indexing
- **Standards-compliant**: Strong SQL standard compliance
- **Open source**: Active community and development

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.
