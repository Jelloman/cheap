# DAO Deletion Fix Plan

## Problem Statement

Integration tests are failing for `testDeleteCatalogCleansUpCustomTables()` in all three database implementations:
- PostgresDaoAspectTableMappingTest
- SqliteDaoAspectTableMappingTest
- MariaDbDaoAspectTableMappingTest

**Error**: Expected 0 rows after catalog deletion, but found 1 row remaining.

**Root Cause**: DAO classes (PostgresDao, SqliteDao, MariaDbDao) are not properly cleaning up AspectTableMapping custom tables when `deleteCatalog()` is called.

## AspectTableMapping Patterns

Different patterns require different cleanup strategies:

### Pattern 1: No IDs (hasCatalogId=false, hasEntityId=false)
- Example: metadata table (key, value)
- Cleanup: **TRUNCATE** entire table (no filtering possible)
- Used by: MariaDbDaoAspectTableMappingTest

### Pattern 2: Catalog ID only (hasCatalogId=true, hasEntityId=false)
- Example: category table (catalog_id, category_name, description)
- Cleanup: **DELETE WHERE catalog_id = ?**
- Used by: SqliteDaoAspectTableMappingTest

### Pattern 3: Entity ID only (hasCatalogId=false, hasEntityId=true)
- Example: person table (entity_id, name, age)
- Example: product table (entity_id, sku, name, price)
- Example: employee table (entity_id, employee_id, name, department)
- Cleanup: **TRUNCATE** entire table (no catalog filtering)
- Used by: PostgresDaoAspectTableMappingTest, SqliteDaoAspectTableMappingTest, MariaDbDaoAspectTableMappingTest

### Pattern 4: Both IDs (hasCatalogId=true, hasEntityId=true)
- Example: settings table (catalog_id, entity_id, key, value)
- Cleanup: **DELETE WHERE catalog_id = ?**
- Used by: PostgresDaoAspectTableMappingTest

## Solution

Each DAO's `deleteCatalog()` method needs to call `clearMappedTable()` for each registered AspectTableMapping.

### Method to Call

```java
protected void clearMappedTable(Connection conn, AspectTableMapping mapping, UUID catalogId) throws SQLException
```

This method already exists in AbstractCheapDao and handles all 4 patterns correctly.

## Implementation Plan

### Step 1: Examine Current deleteCatalog() Implementation

**Files to examine:**
- `D:\src\claude\cheap\cheap-db-postgres\src\main\java\net\netbeing\cheap\db\postgres\PostgresDao.java`
- `D:\src\claude\cheap\cheap-db-sqlite\src\main\java\net\netbeing\cheap\db\sqlite\SqliteDao.java`
- `D:\src\claude\cheap\cheap-db-mariadb\src\main\java\net\netbeing\cheap\db\mariadb\MariaDbDao.java`

**Look for:**
- Current `deleteCatalog(UUID catalogId)` implementation
- Whether it calls `clearMappedTable()` for AspectTableMappings
- Transaction handling

### Step 2: Verify clearMappedTable() Method Exists

**File to check:**
- `D:\src\claude\cheap\cheap-core\src\main\java\net\netbeing\cheap\db\AbstractCheapDao.java`

**Verify:**
- Method signature: `clearMappedTable(Connection, AspectTableMapping, UUID)`
- Handles all 4 AspectTableMapping patterns correctly
- Uses appropriate SQL based on hasCatalogId/hasEntityId flags

### Step 3: Modify deleteCatalog() in PostgresDao

**Location:** `cheap-db-postgres/src/main/java/net/netbeing/cheap/db/postgres/PostgresDao.java`

**Changes needed:**
```java
@Override
public void deleteCatalog(UUID catalogId) throws SQLException
{
    try (Connection conn = dataSource.getConnection())
    {
        conn.setAutoCommit(false);
        try
        {
            // NEW: Clear all mapped tables before deleting catalog
            for (AspectTableMapping mapping : aspectTableMappings.values())
            {
                clearMappedTable(conn, mapping, catalogId);
            }

            // Existing deletion logic...
            // Delete from standard tables

            conn.commit();
        }
        catch (SQLException e)
        {
            conn.rollback();
            throw e;
        }
    }
}
```

### Step 4: Modify deleteCatalog() in SqliteDao

**Location:** `cheap-db-sqlite/src/main/java/net/netbeing/cheap/db/sqlite/SqliteDao.java`

**Changes needed:**
- Same pattern as PostgresDao
- Iterate through all AspectTableMappings
- Call `clearMappedTable()` for each before deleting from standard tables

### Step 5: Modify deleteCatalog() in MariaDbDao

**Location:** `cheap-db-mariadb/src/main/java/net/netbeing/cheap/db/mariadb/MariaDbDao.java`

**Changes needed:**
- Same pattern as PostgresDao and SqliteDao
- Ensure foreign key constraints are handled properly if enabled

### Step 6: Run Integration Tests

**Test command:**
```bash
./gradlew integration-tests
```

**Expected results:**
- All 11 tests should pass
- Deletion tests should verify 0 rows remain in custom tables after catalog deletion

### Step 7: Verify Each Pattern

**Pattern 1 (No IDs):**
- MariaDB metadata table: Should be truncated
- Verify COUNT(*) = 0 after deletion

**Pattern 2 (Catalog ID only):**
- SQLite category table: Should delete by catalog_id
- Verify COUNT(*) WHERE catalog_id = ? returns 0

**Pattern 3 (Entity ID only):**
- PostgreSQL person table: Should be truncated
- SQLite product table: Should be truncated
- MariaDB employee table: Should be truncated
- Verify COUNT(*) = 0 after deletion for all

**Pattern 4 (Both IDs):**
- PostgreSQL settings table: Should delete by catalog_id
- Verify COUNT(*) WHERE catalog_id = ? returns 0

## Testing Strategy

### Before Fix
```
11 tests run
8 passing
3 failing (deletion tests)
```

### After Fix
```
11 tests run
11 passing
0 failing
```

### Specific Tests to Verify

1. **PostgresDaoAspectTableMappingTest::testDeleteCatalogCleansUpCustomTables**
   - Clears person table (Pattern 3 - Entity ID only)
   - Clears settings table (Pattern 4 - Both IDs)

2. **SqliteDaoAspectTableMappingTest::testDeleteCatalogCleansUpCustomTables**
   - Clears product table (Pattern 3 - Entity ID only)
   - Clears category table (Pattern 2 - Catalog ID only)

3. **MariaDbDaoAspectTableMappingTest::testDeleteCatalogCleansUpCustomTables**
   - Clears employee table (Pattern 3 - Entity ID only)
   - Clears metadata table (Pattern 1 - No IDs)

## Commit Strategy

### Commit 1: Fix PostgresDao
```
Fix PostgresDao to clear AspectTableMapping tables on catalog deletion

- Add loop through aspectTableMappings in deleteCatalog()
- Call clearMappedTable() for each mapping before deleting standard tables
- Ensures custom tables are cleaned up for both Pattern 3 and Pattern 4

Tests passing: PostgresDaoAspectTableMappingTest (3/3)
```

### Commit 2: Fix SqliteDao
```
Fix SqliteDao to clear AspectTableMapping tables on catalog deletion

- Add loop through aspectTableMappings in deleteCatalog()
- Call clearMappedTable() for each mapping before deleting standard tables
- Ensures custom tables are cleaned up for Pattern 2 and Pattern 3

Tests passing: SqliteDaoAspectTableMappingTest (3/3)
```

### Commit 3: Fix MariaDbDao
```
Fix MariaDbDao to clear AspectTableMapping tables on catalog deletion

- Add loop through aspectTableMappings in deleteCatalog()
- Call clearMappedTable() for each mapping before deleting standard tables
- Ensures custom tables are cleaned up for Pattern 1 and Pattern 3
- Properly handles foreign key constraints

Tests passing: MariaDbDaoAspectTableMappingTest (4/4)
```

### Final Commit: Verify all tests pass
```
Verify all integration tests pass after DAO deletion fixes

All 11 integration tests now passing:
- PostgresDaoAspectTableMappingTest: 3/3 ✓
- SqliteDaoAspectTableMappingTest: 3/3 ✓
- MariaDbDaoAspectTableMappingTest: 4/4 ✓
- IntegrationTestsPlaceholder: 1/1 ✓

AspectTableMapping cleanup verified for all 4 patterns across all databases.
```

## Success Criteria

✅ All 11 integration tests pass
✅ Deletion tests verify 0 rows in custom tables after catalog deletion
✅ All 4 AspectTableMapping patterns handled correctly
✅ Transaction rollback works properly on errors
✅ Foreign key constraints respected (MariaDB)
✅ No orphaned data in custom tables

## Notes

- The `clearMappedTable()` method in AbstractCheapDao already handles the complexity of different patterns
- DAOs just need to call it for each registered mapping
- Order matters: Clear mapped tables **before** deleting from standard tables to avoid foreign key violations
- All operations should be within same transaction for atomicity
