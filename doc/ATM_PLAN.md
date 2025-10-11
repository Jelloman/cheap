# AspectTableMapping Enhancement Plan

## Overview

This document outlines the plan for enhancing `AspectTableMapping` to support flexible table structures with optional `catalog_id` and `entity_id` columns. These enhancements will allow AspectTableMapping to support various database table patterns, from fully catalog-scoped tables to generic lookup tables.

**Scope:** All code changes are limited to the `cheap-db` module only. No changes will be made to `cheap-core` or `cheap-json`.

## Current State

### AspectTableMapping (AspectTableMapping.java:17-86)
Currently contains:
- `String aspectDefName` - Name of the AspectDef
- `String tableName` - Database table name
- `Map<String, String> propertyToColumnMap` - Property-to-column mappings

The class is used by both `PostgresDao` and `SqliteDao` to save/load aspects from custom database tables instead of the generic `aspect`/`property_value` tables.

### Current Table Assumptions
- Tables **always** have an `entity_id` column as the primary key
- Tables are **always** scoped to a specific entity (1:1 entity-to-row relationship)
- No support for catalog-scoped tables or generic lookup tables

### Existing Test Infrastructure
The migration script `V3__aspect_table_mapping_tests.sql` already creates four test tables:
- `test_aspect_mapping_no_key` - No primary key, no catalog_id, no entity_id
- `test_aspect_mapping_with_cat_id` - Has catalog_id but no primary key, no entity_id
- `test_aspect_mapping_with_entity_id` - No catalog_id, no entity_id (will be added by new code)
- `test_aspect_mapping_with_both_ids` - Has PRIMARY KEY (catalog_id, entity_id)

## Proposed Changes

### 1. Change `aspectDefName` from String to AspectDef Reference

**Location:** AspectTableMapping.java:33

**Current:**
```java
private final String aspectDefName;
```

**Proposed:**
```java
private final AspectDef aspectDef;
```

**Rationale:**
- Provides direct access to property definitions and metadata
- Eliminates need for string-based lookups in DAO code
- Type-safe reference to the AspectDef
- More consistent with Cheap model design principles

**Changes Required:**
- Update constructor parameter from `String aspectDefName` to `AspectDef aspectDef`
- Update getter method `aspectDefName()` to `aspectDef()`
- **Remove/retire the string-based constructor entirely** (no deprecation period)
- Update all usages in PostgresDao and SqliteDao to use AspectDef-based constructor
- Update all test code in PostgresDaoTest to use AspectDef-based constructor
- Update AspectTableMapping registration in DAOs (currently keyed by aspectDefName)

### 2. Add Boolean Flags for Column Presence

**Location:** AspectTableMapping.java (new instance variables)

**Add:**
```java
private final boolean hasCatalogId;
private final boolean hasEntityId;
```

**Rationale:**
These flags control the table structure and persistence behavior:

| hasCatalogId | hasEntityId | Primary Key | Use Case | Data Lifecycle |
|--------------|-------------|-------------|----------|----------------|
| false | false | None | Generic lookup table shared across all catalogs | Truncate table before save |
| true | false | None | Catalog-scoped lookup table | DELETE WHERE catalog_id = ? before save |
| false | true | entity_id | Entity-scoped table (current behavior) | UPDATE/INSERT per entity |
| true | true | (catalog_id, entity_id) | Catalog+Entity scoped table | UPDATE/INSERT per entity |

**Note:** The primary key is NEVER `catalog_id` alone. It is either `entity_id`, `(catalog_id, entity_id)`, or no primary key at all.

**Default Values:**
- `hasCatalogId = false`
- `hasEntityId = true` (maintains backward compatibility)

### 3. Add `createTable()` Methods to DAOs

**Locations:**
- PostgresDao.java (new method)
- SqliteDao.java (new method)

**Add:**
```java
/**
 * Creates a database table based on an AspectTableMapping configuration.
 * The table structure varies based on the mapping's flags:
 * - If hasCatalogId && hasEntityId: PRIMARY KEY (catalog_id, entity_id)
 * - If hasCatalogId only: PRIMARY KEY (catalog_id)
 * - If hasEntityId only: PRIMARY KEY (entity_id)
 * - If neither: No primary key
 *
 * @param mapping the AspectTableMapping defining the table structure
 * @throws SQLException if table creation fails
 */
public void createTable(@NotNull AspectTableMapping mapping) throws SQLException
```

**Implementation Details:**

PostgreSQL:
```sql
CREATE TABLE IF NOT EXISTS <tableName> (
    [catalog_id UUID [NOT NULL],]        -- if hasCatalogId
    [entity_id UUID [NOT NULL],]         -- if hasEntityId
    <property columns based on AspectDef PropertyDefs>
    [PRIMARY KEY (entity_id)]            -- if hasEntityId && !hasCatalogId
    [PRIMARY KEY (catalog_id, entity_id) -- if hasEntityId && hasCatalogId]
)
```

**Note:** Always use `CREATE TABLE IF NOT EXISTS` to make the operation idempotent.

SQLite:
```sql
CREATE TABLE IF NOT EXISTS <tableName> (
    [catalog_id TEXT [NOT NULL],]        -- if hasCatalogId
    [entity_id TEXT [NOT NULL],]         -- if hasEntityId
    <property columns based on AspectDef PropertyDefs>
    [PRIMARY KEY (entity_id)]            -- if hasEntityId && !hasCatalogId
    [PRIMARY KEY (catalog_id, entity_id) -- if hasEntityId && hasCatalogId]
)
```

**Note:** Always use `CREATE TABLE IF NOT EXISTS` to make the operation idempotent.

**Column Type Mapping:**
Use existing `mapPropertyTypeToSqlType()` methods to convert PropertyType to database column types.

**NOT NULL Constraints:**
- `catalog_id`: NOT NULL if `hasCatalogId`
- `entity_id`: NOT NULL if `hasEntityId`
- Property columns: Based on PropertyDef.isNullable()

### 4. Modify Save Behavior in DAOs

**Locations:**
- PostgresDao.saveAspectMapContentToMappedTable() (line 627)
- SqliteDao.saveAspectMapContentToMappedTable() (line 734)

**Current Behavior:**
Always performs `INSERT ... ON CONFLICT (entity_id) DO UPDATE` for each entity in the hierarchy.

**Proposed Behavior:**

```java
private void saveAspectMapContentToMappedTable(
    Connection conn,
    UUID/String catalogId,  // UUID for Postgres, String for SQLite
    String hierarchyName,
    AspectMapHierarchy hierarchy,
    AspectTableMapping mapping) throws SQLException
{
    // Step 1: Pre-save cleanup based on flags
    if (!mapping.hasEntityId() && !mapping.hasCatalogId()) {
        // Truncate entire table (generic lookup table)
        executeSql(conn, "TRUNCATE TABLE " + mapping.tableName());
    } else if (mapping.hasCatalogId() && !mapping.hasEntityId()) {
        // Delete all rows for this catalog
        executeSql(conn, "DELETE FROM " + mapping.tableName() + " WHERE catalog_id = ?", catalogId);
    }
    // else: entity_id is present, so use UPDATE/INSERT per entity (no pre-cleanup)

    // Step 2: Build column list for INSERT
    StringBuilder columns = new StringBuilder();
    StringBuilder placeholders = new StringBuilder();

    if (mapping.hasCatalogId()) {
        columns.append("catalog_id");
        placeholders.append("?");
    }

    if (mapping.hasEntityId()) {
        if (columns.length() > 0) columns.append(", ");
        if (placeholders.length() > 0) placeholders.append(", ");
        columns.append("entity_id");
        placeholders.append("?");
    }

    for (Map.Entry<String, String> entry : mapping.propertyToColumnMap().entrySet()) {
        columns.append(", ").append(entry.getValue());
        placeholders.append(", ?");
    }

    // Step 3: Build INSERT statement with appropriate conflict handling
    StringBuilder sql = new StringBuilder("INSERT INTO ")
        .append(mapping.tableName())
        .append(" (").append(columns).append(") VALUES (").append(placeholders).append(")");

    // Add ON CONFLICT clause based on primary key configuration
    if (mapping.hasEntityId()) {
        sql.append(" ON CONFLICT (");
        if (mapping.hasCatalogId()) {
            sql.append("catalog_id, entity_id");
        } else {
            sql.append("entity_id");
        }
        sql.append(") DO UPDATE SET ");

        // Build UPDATE clause for all property columns
        boolean first = true;
        for (String columnName : mapping.propertyToColumnMap().values()) {
            if (!first) sql.append(", ");
            sql.append(columnName).append(" = EXCLUDED.").append(columnName);
            first = false;
        }
    }

    // Step 4: Execute INSERT for each entity/aspect in hierarchy
    try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
        for (Entity entity : hierarchy.keySet()) {
            if (mapping.hasEntityId()) {
                saveEntity(conn, entity);  // Ensure entity is in entity table
            }

            Aspect aspect = hierarchy.get(entity);
            if (aspect != null) {
                int paramIndex = 1;

                if (mapping.hasCatalogId()) {
                    stmt.setObject(paramIndex++, catalogId);
                }

                if (mapping.hasEntityId()) {
                    stmt.setObject(paramIndex++, entity.globalId());
                }

                for (Map.Entry<String, String> entry : mapping.propertyToColumnMap().entrySet()) {
                    String propName = entry.getKey();
                    Object value = aspect.readObj(propName);
                    PropertyDef propDef = aspect.def().propertyDef(propName);
                    if (propDef != null) {
                        setPropertyValue(stmt, paramIndex++, value, propDef.type());
                    } else {
                        stmt.setObject(paramIndex++, value);
                    }
                }

                stmt.executeUpdate();
            }
        }
    }
}
```

**Note on Entity Generation:**
When `hasEntityId = false`, entity IDs are **not** persisted to the table. They will be generated from scratch each time rows are loaded. This means:
- Entities will have different UUIDs on each load
- Entity identity is not preserved across save/load cycles
- This is acceptable for lookup tables where entity identity doesn't matter

### 5. Modify Load Behavior in DAOs

**Locations:**
- PostgresDao.loadAspectMapContentFromMappedTable() (line 1065)
- SqliteDao.loadAspectMapContentFromMappedTable() (line 1178)

**Current Behavior:**
Reads all rows from the table and creates entities based on `entity_id` column.

**Proposed Behavior:**

```java
private void loadAspectMapContentFromMappedTable(
    Connection conn,
    UUID/String catalogId,
    String hierarchyName,
    AspectMapHierarchy hierarchy,
    AspectTableMapping mapping) throws SQLException
{
    // Step 1: Build column list for SELECT
    StringBuilder columns = new StringBuilder();

    if (mapping.hasCatalogId()) {
        columns.append("catalog_id");
    }

    if (mapping.hasEntityId()) {
        if (columns.length() > 0) columns.append(", ");
        columns.append("entity_id");
    }

    for (String columnName : mapping.propertyToColumnMap().values()) {
        columns.append(", ").append(columnName);
    }

    // Step 2: Build SELECT statement with optional WHERE clause
    StringBuilder sql = new StringBuilder("SELECT ")
        .append(columns)
        .append(" FROM ")
        .append(mapping.tableName());

    if (mapping.hasCatalogId()) {
        sql.append(" WHERE catalog_id = ?");
    }

    // Step 3: Execute query
    try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
        if (mapping.hasCatalogId()) {
            stmt.setObject(1, catalogId);
        }

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                // Step 4: Get or create entity
                Entity entity;
                if (mapping.hasEntityId()) {
                    UUID entityId = UUID.fromString(rs.getString("entity_id"));
                    entity = factory.getOrRegisterNewEntity(entityId);
                } else {
                    // Generate new entity ID for this row (not persisted)
                    entity = factory.createEntity();
                }

                // Step 5: Create aspect and populate properties
                Aspect aspect = factory.createPropertyMapAspect(entity, mapping.aspectDef());

                for (Map.Entry<String, String> entry : mapping.propertyToColumnMap().entrySet()) {
                    String propName = entry.getKey();
                    String columnName = entry.getValue();

                    PropertyDef propDef = mapping.aspectDef().propertyDef(propName);
                    if (propDef != null) {
                        Object value = rs.getObject(columnName);

                        // Handle type conversions (especially UUID from TEXT in SQLite)
                        if (propDef.type() == PropertyType.UUID && value != null) {
                            value = UUID.fromString(value.toString());
                        }

                        Property property = factory.createProperty(propDef, value);
                        aspect.put(property);
                    }
                }

                hierarchy.put(entity, aspect);
            }
        }
    }
}
```

## Implementation Checklist

### Phase 1: AspectTableMapping Changes
- [ ] Change `aspectDefName` field from String to AspectDef
- [ ] Add `hasCatalogId` boolean field (default: false)
- [ ] Add `hasEntityId` boolean field (default: true)
- [ ] Update constructor to accept AspectDef instead of String
- [ ] Update constructor to accept hasCatalogId and hasEntityId flags
- [ ] **Remove** the string-based constructor entirely (no backward compatibility)
- [ ] Update `aspectDefName()` getter to `aspectDef()` returning AspectDef
- [ ] Add `hasCatalogId()` getter
- [ ] Add `hasEntityId()` getter
- [ ] Update JavaDoc with new behavior descriptions

### Phase 2: Update Existing Callers
- [ ] Update PostgresDaoTest.testAspectTableMapping() to use AspectDef-based constructor
- [ ] Update PostgresDaoTest.testCreateAspectTableWithAllPropertyTypes() to use AspectDef-based constructor
- [ ] Update any other test code using AspectTableMapping string-based constructor

### Phase 3: PostgresDao Changes
- [ ] Update `addAspectTableMapping()` to use aspectDef.name() for map key
- [ ] Update `createAspectTable()` to be deprecated (use new `createTable()` instead)
- [ ] Add new `createTable(AspectTableMapping)` method with CREATE IF NOT EXISTS
- [ ] Implement table creation with flexible column structure
- [ ] Update `saveAspectMapContentToMappedTable()` with pre-save cleanup logic
- [ ] Update `saveAspectMapContentToMappedTable()` with flexible column handling
- [ ] Update `loadAspectMapContentFromMappedTable()` with flexible column handling
- [ ] Update `loadAspectMapContentFromMappedTable()` to support entity generation

### Phase 4: SqliteDao Changes
- [ ] Update `addAspectTableMapping()` to use aspectDef.name() for map key
- [ ] Update `createAspectTable()` to be deprecated (use new `createTable()` instead)
- [ ] Add new `createTable(AspectTableMapping)` method with CREATE IF NOT EXISTS
- [ ] Implement table creation with flexible column structure
- [ ] Update `saveAspectMapContentToMappedTable()` with pre-save cleanup logic
- [ ] Update `saveAspectMapContentToMappedTable()` with flexible column handling
- [ ] Update `loadAspectMapContentFromMappedTable()` with flexible column handling
- [ ] Update `loadAspectMapContentFromMappedTable()` to support entity generation

### Phase 5: Update V3 Migration Script
- [ ] Add INSERT statements to V3__aspect_table_mapping_tests.sql
- [ ] Insert 2 test rows into test_aspect_mapping_no_key
- [ ] Insert 2 test rows into test_aspect_mapping_with_cat_id
- [ ] Insert 2 test rows into test_aspect_mapping_with_entity_id
- [ ] Insert 2 test rows into test_aspect_mapping_with_both_ids
- [ ] Fix syntax error in test_aspect_mapping_with_cat_id (trailing comma on line 22)

### Phase 6: Testing
- [ ] Update existing AspectTableMapping tests to use AspectDef reference
- [ ] Add test for table with no catalog_id and no entity_id (generic lookup)
- [ ] Add test for table with catalog_id but no entity_id (catalog-scoped lookup)
- [ ] Add test for table with entity_id but no catalog_id (current behavior)
- [ ] Add test for table with both catalog_id and entity_id (composite key)
- [ ] Add test for createTable() with each flag combination
- [ ] Add test verifying truncate behavior (no IDs)
- [ ] Add test verifying DELETE WHERE catalog_id behavior
- [ ] Add test verifying entity generation on load when hasEntityId=false
- [ ] Add test for save/load round-trip with each flag combination

### Phase 7: Documentation
- [ ] Update AspectTableMapping JavaDoc
- [ ] Update PostgresDao JavaDoc for createTable() method
- [ ] Update SqliteDao JavaDoc for createTable() method
- [ ] Update PostgresDao class-level JavaDoc with new table mapping patterns
- [ ] Update SqliteDao class-level JavaDoc with new table mapping patterns
- [ ] Add code examples to JavaDoc showing different table patterns

## Usage Examples

### Example 1: Generic Lookup Table (No IDs)
```java
// A currency exchange rate table shared across all catalogs
AspectDef currencyRateDef = factory.createImmutableAspectDef("currency_rate", Map.of(
    "from_currency", factory.createPropertyDef("from_currency", PropertyType.String, ...),
    "to_currency", factory.createPropertyDef("to_currency", PropertyType.String, ...),
    "rate", factory.createPropertyDef("rate", PropertyType.Float, ...)
));

AspectTableMapping mapping = new AspectTableMapping(
    currencyRateDef,
    "currency_rates",
    Map.of("from_currency", "from_currency", "to_currency", "to_currency", "rate", "rate"),
    false,  // hasCatalogId
    false   // hasEntityId
);

dao.createTable(mapping);
dao.addAspectTableMapping(mapping);

// When saved: TRUNCATE TABLE currency_rates before inserting rows
// When loaded: New entity IDs generated for each row
```

### Example 2: Catalog-Scoped Lookup Table (No Primary Key)
```java
// Product categories specific to each catalog
AspectDef categoryDef = factory.createImmutableAspectDef("product_category", Map.of(
    "category_code", factory.createPropertyDef("category_code", PropertyType.String, ...),
    "category_name", factory.createPropertyDef("category_name", PropertyType.String, ...)
));

AspectTableMapping mapping = new AspectTableMapping(
    categoryDef,
    "product_categories",
    Map.of("category_code", "category_code", "category_name", "category_name"),
    true,   // hasCatalogId
    false   // hasEntityId
);

dao.createTable(mapping);  // Creates table WITHOUT primary key
dao.addAspectTableMapping(mapping);

// When saved: DELETE FROM product_categories WHERE catalog_id = ? before inserting
// When loaded: Only rows for the current catalog, new entity IDs generated
// Note: No primary key constraint - catalog_id is used for filtering only
```

### Example 3: Entity-Scoped Table (Current Behavior)
```java
// Customer data with persistent entity identity
AspectDef customerDef = factory.createImmutableAspectDef("customer", Map.of(
    "name", factory.createPropertyDef("name", PropertyType.String, ...),
    "email", factory.createPropertyDef("email", PropertyType.String, ...)
));

AspectTableMapping mapping = new AspectTableMapping(
    customerDef,
    "customers",
    Map.of("name", "customer_name", "email", "email_address"),
    false,  // hasCatalogId
    true    // hasEntityId (default)
);

dao.createTable(mapping);
dao.addAspectTableMapping(mapping);

// When saved: INSERT ... ON CONFLICT (entity_id) DO UPDATE for each entity
// When loaded: Entity IDs preserved from entity_id column
```

### Example 4: Catalog+Entity Scoped Table
```java
// User preferences scoped to both catalog and entity
AspectDef userPrefDef = factory.createImmutableAspectDef("user_preferences", Map.of(
    "theme", factory.createPropertyDef("theme", PropertyType.String, ...),
    "language", factory.createPropertyDef("language", PropertyType.String, ...)
));

AspectTableMapping mapping = new AspectTableMapping(
    userPrefDef,
    "user_prefs",
    Map.of("theme", "theme", "language", "language"),
    true,   // hasCatalogId
    true    // hasEntityId
);

dao.createTable(mapping);
dao.addAspectTableMapping(mapping);

// When saved: INSERT ... ON CONFLICT (catalog_id, entity_id) DO UPDATE
// When loaded: Only rows for current catalog, entity IDs preserved
```

## Backward Compatibility

**Breaking Change:** The string-based constructor will be **removed entirely** with no deprecation period. All existing callers will be updated in the same commit to use the AspectDef-based constructor.

### Migration Path
1. Identify all existing usages of AspectTableMapping constructor (in cheap-db only)
2. Update each call site to pass AspectDef instead of String
3. Remove the string-based constructor
4. Ensure all tests pass

This is acceptable because:
- AspectTableMapping is relatively new and has limited usage
- All usage is within the cheap-db module (which we control)
- The migration is straightforward and mechanical

## Risks and Considerations

### 1. Entity ID Generation
When `hasEntityId = false`, entity IDs are generated on each load. This means:
- **Risk:** Entity references across hierarchies will break
- **Mitigation:** Only use this mode for true lookup tables without cross-references
- **Documentation:** Clearly document this limitation in JavaDoc

### 2. Catalog Isolation
When `hasCatalogId = true`, only rows for the current catalog are loaded:
- **Risk:** Shared reference data might need to be duplicated per catalog
- **Mitigation:** Use `hasCatalogId = false` for truly shared data
- **Documentation:** Explain catalog scoping behavior

### 3. Data Loss Risk
The truncate/delete behavior could cause data loss:
- **Risk:** Accidentally truncating important data
- **Mitigation:** Require explicit flag settings in constructor
- **Documentation:** Warn about destructive operations in JavaDoc

### 4. Backward Compatibility
Changing aspectDefName to aspectDef is a breaking change:
- **Risk:** Existing code will not compile
- **Mitigation:** Update all existing usages before merging
- **Testing:** Ensure all tests pass after migration

## Open Questions

1. **Should we support partial property mappings?**
   - Currently assumes all properties map to columns
   - Could allow some properties to be stored in default tables
   - **Status:** Deferred to future enhancement

2. **Should we support composite primary keys beyond catalog_id + entity_id?**
   - e.g., (catalog_id, product_code) as natural key
   - **Status:** Deferred to future enhancement

3. **Should we add support for foreign key relationships?**
   - Could extend mapping to specify FK constraints
   - **Status:** Deferred to future enhancement

4. ~~**Should createTable() be idempotent (CREATE IF NOT EXISTS)?**~~
   - **RESOLVED:** Yes, always use `CREATE TABLE IF NOT EXISTS`
   - This makes the operation safe to call multiple times
   - Aligns with best practices for schema management

## Future Enhancements

1. **Support for views** - Allow AspectTableMapping to reference views, not just tables
2. **Column name transformation** - Support for case conversion (snake_case, camelCase)
3. **Computed columns** - Support for loading columns that are computed/derived
4. **Batch operations** - Optimize save/load for large datasets
5. **Schema migration support** - Handle adding/removing columns over time
