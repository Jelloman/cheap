# Integration Test Failures - Phase 4 (Updated)

**Initial Test Run Date**: 2025-11-10
**Updated After Fix**: 2025-11-10

## Initial Results

**Total Tests**: 36
**Passed**: 18
**Failed**: 18

## Current Results (After MariaDB Connection Fix)

**Total Tests**: 36
**Passed**: 22
**Failed**: 14
**Improvement**: 4 tests fixed (22% reduction in failures)

---

## Summary by Test Suite

| Test Suite | Passed | Failed | Total | Status |
|-----------|--------|--------|-------|--------|
| SqliteDaoAspectTableMappingTest | 3 | 0 | 3 | ✅ All Pass |
| MariaDbDaoAspectTableMappingTest | 4 | 0 | 4 | ✅ All Pass |
| PostgresDaoAspectTableMappingTest | 3 | 0 | 3 | ✅ All Pass |
| **MariaDbRestClientIntegrationTest** | **4** | **5** | **9** | ⚠️ Improved (was 0/9) |
| **SqliteRestClientIntegrationTest** | **4** | **5** | **9** | ⚠️ Partial |
| **PostgresRestClientIntegrationTest** | **4** | **4** | **8** | ⚠️ Partial |

---

## DAO Tests - All Passing ✅

All DAO-level integration tests passed successfully:
- Custom table mapping works correctly
- Database cleanup functions properly
- Foreign key constraints are enforced (MariaDB)

---

## MariaDB Connection Issue - RESOLVED ✅

### Problem #1 - FIXED

**Initial Error**: `java.sql.SQLNonTransientConnectionException: Socket fail to connect to localhost. Connection refused: getsockopt`

**Root Cause**: Port configuration mismatch
- MariaDB4j was configured with `.setPort(0)` to use a random available port
- Spring Boot configuration had a hardcoded URL `jdbc:mariadb://localhost:3306/test`
- MariaDB4j started on port 20345 (random), but Spring Boot tried to connect to port 3306
- Result: Connection refused error

**Solution Applied**: Added `@DynamicPropertySource` method to `MariaDbRestIntegrationTest.java`
- Dynamically sets `spring.datasource.url` based on actual MariaDB4j port
- Similar to how SQLite tests handle dynamic configuration

**Files Modified**:
- `integration-tests/src/integration/java/net/netbeing/cheap/integrationtests/base/MariaDbRestIntegrationTest.java`

**Result**: Connection now succeeds, MariaDB tests can connect to database

---

### Problem #2 - FIXED

**Error**: `java.sql.SQLSyntaxErrorException: (conn=44) Unknown database 'test'`

**Root Cause**: Database not created before Spring Boot startup
- @DynamicPropertySource ran before database was created
- Spring Boot tried to connect to non-existent database

**Solution Applied**: Added database creation in `@DynamicPropertySource` method
- Creates "test" database before Spring Boot starts
- Handles case where database already exists from previous test class

**Result**: Database exists when Spring Boot starts

---

### Problem #3 - FIXED

**Error**: `500 Internal Server Error` when creating catalogs

**Root Cause**: Database schema not initialized
- Database existed but lacked Cheap schema tables
- MariaDbCheapSchema DDL was not executed for the "test" database

**Solution Applied**: Added schema initialization in `@DynamicPropertySource` method
- Executes `MariaDbCheapSchema.executeMainSchemaDdl()`
- Executes `MariaDbCheapSchema.executeForeignKeysDdl()`
- Executes `MariaDbCheapSchema.executeAuditSchemaDdl()`
- Handles case where schema already exists

**Result**: Database has proper schema, catalog creation succeeds

---

### MariaDB Tests Now Passing

The following MariaDB tests are now **PASSING** (4/9):
- ✅ `aspectDefCRUD` - Create, retrieve, and list aspect definitions
- ✅ `entityListHierarchy` - Entity list with pagination
- ✅ `errorHandling` - 404 error responses
- ✅ `entityDirectoryHierarchy` - Entity directory operations

The following MariaDB tests are still **FAILING** (5/9):
- ❌ `aspectMapHierarchy` - Same issue as SQLite/Postgres
- ❌ `aspectUpsert` - Same issue as SQLite/Postgres
- ❌ `catalogLifecycle` - Same issue as SQLite/Postgres
- ❌ `customTableMapping` - Same issue as SQLite/Postgres
- ❌ `foreignKeyConstraints` - New issue (possibly FK-specific)

---

## REST Client Test Failures (Remaining)

---

### 2. SqliteRestClientIntegrationTest - Partial Failure (5/9 Failed)

#### Failure 1: `aspectMapHierarchy`

**Error**: `java.lang.UnsupportedOperationException: A hierarchy may not be added to a Catalog with the same name as an existing AspectMapHierarchy.`

**Location**: `CatalogImpl.addHierarchy()` line 227, called from `SqliteRestClientIntegrationTest.aspectMapHierarchy()` line 346

**Diagnosis**:
- Test attempts to create a hierarchy with a name that already exists as an AspectMapHierarchy in the catalog
- This is a validation constraint - catalog prevents duplicate hierarchy names
- Likely test setup issue where test doesn't clean up properly or creates duplicate hierarchies

**Suggested Solution**:
1. Review test at line 346 to ensure unique hierarchy names
2. Add cleanup between test steps to remove existing hierarchies
3. Consider if this is expected behavior - test may need to be restructured to avoid name collision

---

#### Failure 2: `aspectUpsert`

**Error**: `net.netbeing.cheap.rest.client.exception.CheapRestClientException: Request failed: JSON decoding error: Attempted to read property named 'sku' with any aspect or property definition.`

**Location**: `AspectDeserializer.deserialize()` line 120, called from `SqliteRestClientIntegrationTest.aspectUpsert()` line 234

**Diagnosis**:
- AspectDeserializer encounters a property named 'sku' in the JSON response
- No AspectDef or PropertyDef exists in the catalog to define what 'sku' should be
- The test upserts aspects with a 'product' AspectDef but the server returns aspects containing 'sku' property
- Mismatch between what was created (AspectDef schema) and what the server returned

**Suggested Solution**:
1. Ensure the test creates a complete AspectDef with all required PropertyDefs including 'sku'
2. Verify the AspectDef creation at the test setup includes all properties that will be returned
3. Check that the catalog state is properly initialized before the query
4. Review JSON deserialization to ensure PropertyDef metadata is available during deserialization

---

#### Failure 3: `customTableMapping`

**Error**: `net.netbeing.cheap.rest.client.exception.CheapRestServerException: Server error: 500 Internal Server Error from POST http://localhost:20440/api/catalog/.../aspect-defs`

**Root Cause**: `org.springframework.core.codec.DecodingException: JSON decoding error: Attempted to deserialize AspectDef order_item that conflicts with the AspectDef already registered with that name.`

**Location**: `AspectDefDeserializer.deserialize()` line 146, called from `SqliteRestClientIntegrationTest.customTableMapping()` line 152

**Diagnosis**:
- Test attempts to create AspectDef named 'order_item'
- An AspectDef with the name 'order_item' already exists in the catalog from a previous test
- Test isolation issue - catalog state is leaking between tests
- Tests are not properly cleaning up AspectDefs or are reusing the same catalog instance

**Suggested Solution**:
1. Ensure each test creates a fresh catalog or cleans up AspectDefs in teardown
2. Review test setup/teardown methods to ensure proper isolation
3. Use unique AspectDef names per test or delete catalog between tests
4. Consider using `@DirtiesContext` to force Spring context reload between tests

---

#### Failure 4: `catalogLifecycle`

**Error**: `net.netbeing.cheap.rest.client.exception.CheapRestClientException: HTTP error 200 OK: ... java.lang.NullPointerException: Cannot invoke "String.hashCode()" because "<local6>" is null`

**Location**: `CatalogDefDeserializer.deserialize()` line 82, called from `SqliteRestClientIntegrationTest.catalogLifecycle()` line 92

**Diagnosis**:
- Server returns HTTP 200 OK but JSON deserialization fails
- CatalogDefDeserializer encounters a null string value at line 82 when trying to compute hashCode
- The variable `<local6>` (likely a string field in CatalogDef) is unexpectedly null
- Server serialized a CatalogDef with a null field that should not be null
- Indicates incomplete CatalogDef construction or missing required field validation

**Suggested Solution**:
1. Review CatalogDefDeserializer line 82 to identify which field is null
2. Add null checks or default values in deserializer
3. Ensure server-side CatalogDef serialization includes all required fields
4. Add validation to prevent null values in required CatalogDef fields
5. Check if catalog creation is completing properly before attempting to retrieve it

---

#### Failure 5: `executionError`

**Error**: `java.nio.file.FileSystemException: C:\Users\Dave\AppData\Local\Temp\cheap-integration-test-06336812668686489879.db: The process cannot access the file because it is being used by another process`

**Location**: `SqliteRestIntegrationTest.tearDownSqlite()` line 63

**Diagnosis**:
- Test teardown attempts to delete the temporary SQLite database file
- File is still locked by another process (likely SQLite connection pool)
- HikariCP or SQLite JDBC driver hasn't fully closed all connections
- This is not a test failure but a cleanup failure
- May indicate improper connection pool shutdown order

**Suggested Solution**:
1. Ensure HikariCP connection pool is properly closed before file deletion
2. Add explicit shutdown of data source in @AfterEach method before file cleanup
3. Add retry logic with delay for file deletion
4. Call `datasource.close()` and wait for connections to fully release
5. Consider using try-with-resources or shutdown hooks to ensure proper cleanup order

---

### 3. PostgresRestClientIntegrationTest - Partial Failure (4/8 Failed)

The Postgres tests have the **exact same failures** as SQLite tests (failures 1-4 above):

#### Failure 1: `aspectMapHierarchy`
- Same error: Hierarchy name conflict
- Same location: line 346
- Same diagnosis and solution as SQLite

#### Failure 2: `aspectUpsert`
- Same error: Property 'sku' not defined
- Same location: line 234
- Same diagnosis and solution as SQLite

#### Failure 3: `customTableMapping`
- Same error: AspectDef 'order_item' conflict
- Same location: line 152
- Same diagnosis and solution as SQLite

#### Failure 4: `catalogLifecycle`
- Same error: NullPointerException in CatalogDefDeserializer
- Same location: line 92
- Same diagnosis and solution as SQLite

---

## Common Patterns

### Pattern 1: Test Isolation Issues
Multiple tests fail due to state leaking between tests:
- AspectDef name conflicts ('order_item')
- Hierarchy name conflicts

**Fix**: Ensure proper test isolation by creating fresh catalogs or cleaning up between tests.

### Pattern 2: JSON Deserialization Schema Mismatches
Tests fail when deserializing JSON responses because:
- PropertyDef 'sku' is not defined in the catalog
- CatalogDef has null fields

**Fix**: Ensure complete schema definitions are created before operations and validate required fields.

### Pattern 3: Resource Cleanup Timing
- MariaDB connection timing issues
- SQLite file locking in cleanup

**Fix**: Add proper wait conditions and shutdown ordering.

---

## Priority Fixes

### High Priority
1. ~~**Fix MariaDB connection issue**~~ - ✅ **RESOLVED** - Now 4/9 MariaDB tests passing
2. **Fix test isolation** - affects multiple tests across all databases (SQLite, Postgres, MariaDB)
3. **Fix CatalogDefDeserializer NPE** - affects catalog lifecycle tests (SQLite, Postgres, MariaDB)

### Medium Priority
4. **Fix AspectDef schema completeness** - ensure all PropertyDefs are defined
5. **Fix hierarchy name conflicts** - ensure unique names or proper cleanup
6. **Investigate MariaDB foreignKeyConstraints failure** - new issue specific to MariaDB

### Low Priority
7. **Improve SQLite file cleanup** - add proper connection pool shutdown

---

## Test Statistics

### Initial Results (Before Fix)
**Database-Specific Results**:
- SQLite DAO: 3/3 passed ✅
- MariaDB DAO: 4/4 passed ✅
- Postgres DAO: 3/3 passed ✅
- SQLite REST: 4/9 passed ⚠️
- **MariaDB REST: 0/9 passed ❌**
- Postgres REST: 4/8 passed ⚠️

**Overall**: 18/36 tests passing (50% pass rate)

### Current Results (After MariaDB Connection Fix)
**Database-Specific Results**:
- SQLite DAO: 3/3 passed ✅
- MariaDB DAO: 4/4 passed ✅
- Postgres DAO: 3/3 passed ✅
- SQLite REST: 4/9 passed ⚠️
- **MariaDB REST: 4/9 passed ⚠️** (improved from 0/9)
- Postgres REST: 4/8 passed ⚠️

**Overall**: 22/36 tests passing (61% pass rate, up from 50%)

**Improvement**: +4 tests fixed, +11% pass rate increase
