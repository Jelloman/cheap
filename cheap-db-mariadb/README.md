# cheap-db-mariadb

MariaDB persistence layer for the Cheap data caching system.

## Overview

cheap-db-mariadb provides complete MariaDB database persistence for Cheap catalogs, enabling you to save and load entire catalog structures including hierarchies, entities, aspects, and properties.

## Features

- **Complete Catalog Persistence**: Save and load entire catalogs with all contents
- **All Hierarchy Types Supported**: ENTITY_LIST, ENTITY_SET, ENTITY_DIR, ENTITY_TREE, ASPECT_MAP
- **Transaction Management**: Full ACID compliance with automatic rollback on errors
- **Type-Safe Property Storage**: Proper handling of all Cheap property types
- **Schema Management**: Automatic schema creation and migration via Flyway
- **Connection Pooling Support**: Works with HikariCP and other pooling libraries

## Installation

### Gradle

```groovy
dependencies {
    implementation 'net.netbeing:cheap-core:0.1'
    implementation 'net.netbeing:cheap-db-mariadb:0.1'
    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client:3.3.0'
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
        <artifactId>cheap-db-mariadb</artifactId>
        <version>0.1</version>
    </dependency>
    <dependency>
        <groupId>org.mariadb.jdbc</groupId>
        <artifactId>mariadb-java-client</artifactId>
        <version>3.3.0</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

## Quick Start

### Database Setup

Create a MariaDB database and user:

```sql
CREATE DATABASE cheap;
CREATE USER 'cheap_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON cheap.* TO 'cheap_user'@'localhost';
FLUSH PRIVILEGES;
```

### Basic Usage

```java
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.netbeing.cheap.db.mariadb.CatalogDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.*;

// Configure DataSource
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mariadb://localhost:3306/cheap");
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

// Save to MariaDB
dao.saveCatalog(catalog);

// Load from MariaDB
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
config.setJdbcUrl("jdbc:mariadb://localhost:3306/cheap");
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
    url: jdbc:mariadb://localhost:3306/cheap
    username: cheap_user
    password: ${DB_PASSWORD}
    driver-class-name: org.mariadb.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```

### Connection URL Options

Common MariaDB JDBC URL options:

```
jdbc:mariadb://localhost:3306/cheap?
  useSSL=true&
  serverTimezone=UTC&
  allowPublicKeyRetrieval=true
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

| Cheap PropertyType | MariaDB Type      | Notes                    |
|--------------------|-------------------|--------------------------|
| Integer            | BIGINT            | 64-bit signed integer    |
| Float              | DOUBLE            | 64-bit floating point    |
| Boolean            | BOOLEAN           | TRUE/FALSE/NULL          |
| String             | VARCHAR(8192)     | Max 8192 characters      |
| Text               | TEXT              | Unlimited length         |
| BigInteger         | TEXT              | Stored as string         |
| BigDecimal         | TEXT              | Stored as string         |
| DateTime           | VARCHAR(64)       | ISO-8601 format          |
| URI                | VARCHAR(2048)     | URI string               |
| UUID               | CHAR(36)          | Standard UUID format     |
| CLOB               | LONGTEXT          | Character large object   |
| BLOB               | LONGBLOB          | Binary large object      |

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
- Primary keys (all tables)
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

## Error Handling

Common exceptions and solutions:

### SQLException - Connection Failed
```
Cause: Cannot connect to MariaDB server
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

## Migration from PostgreSQL/SQLite

The API is identical across all database backends. To migrate:

1. Export data from old database
2. Configure new MariaDB DataSource
3. Import data using the same code

```java
// Load from PostgreSQL
CatalogDao postgresDao = new CatalogDao(postgresDataSource);
Catalog catalog = postgresDao.loadCatalog(catalogId);

// Save to MariaDB
CatalogDao mariadbDao = new CatalogDao(mariadbDataSource);
mariadbDao.saveCatalog(catalog);
```

## Troubleshooting

### Schema Not Created

If schema isn't auto-created:
1. Check user has CREATE TABLE privileges
2. Check Flyway is on classpath
3. Manually run DDL from `src/main/resources/db/migration/`

### Slow Queries

If queries are slow:
1. Check indexes exist: `SHOW INDEX FROM table_name`
2. Analyze query execution: `EXPLAIN SELECT ...`
3. Optimize table: `OPTIMIZE TABLE table_name`
4. Increase buffer pool size in my.cnf

### Connection Pool Exhaustion

If connection pool is exhausted:
1. Increase pool size in configuration
2. Check for connection leaks (unclosed resources)
3. Reduce connection timeout
4. Monitor connection usage

## Building

```bash
# Build module
./gradlew :cheap-db-mariadb:build

# Run tests (requires MariaDB)
./gradlew :cheap-db-mariadb:test
```

## Related Modules

- **cheap-core** - Core data model (required dependency)
- **cheap-db-postgres** - PostgreSQL implementation
- **cheap-db-sqlite** - SQLite implementation
- **cheap-rest** - REST API that can use this module

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.
