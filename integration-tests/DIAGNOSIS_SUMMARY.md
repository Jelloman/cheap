# Integration Test Diagnosis Summary

## Date: 2025-11-09

This document summarizes the diagnosis and fixes applied to resolve integration test failures.

## Issues Diagnosed and Fixed

### 1. @SpringBootConfiguration Error ✅ FIXED

**Error**: `Unable to find a @SpringBootConfiguration`

**Root Cause**:
- Integration tests in separate module couldn't auto-detect `CheapRestApplication`
- Spring Boot scans starting from test package, not application package

**Fix**:
- Added `classes = CheapRestApplication.class` to `@SpringBootTest` annotation in `BaseRestIntegrationTest`
- **Fixed by**: User (commit a58f62e)

**File**: `integration-tests/src/integration/java/net/netbeing/cheap/integrationtests/base/BaseRestIntegrationTest.java`

---

### 2. YAML Parsing Errors ✅ FIXED

**Error**: `ScannerException: mapping values are not allowed here`

**Root Cause**:
- Unquoted JDBC URLs containing colons (`:`) in YAML files
- YAML parser interpreted `::` as mapping separator in `jdbc:sqlite::memory:`

**Fix**:
- Quoted all JDBC URLs in test configuration files
- **Fixed by**: Claude (commit 9c7655f)

**Files Changed**:
- `integration-tests/src/integration/resources/application-sqlite-test.yml`
- `integration-tests/src/integration/resources/application-postgres-test.yml`
- `integration-tests/src/integration/resources/application-mariadb-test.yml`

**Example**:
```yaml
# Before
url: jdbc:sqlite::memory:

# After
url: "jdbc:sqlite::memory:"
```

---

### 3. Compilation Errors (API Usage) ✅ FIXED

**Errors**: 101 compilation errors initially, then 22 remaining

**Root Causes**:
- Wrong DTO method names (e.g., `aspects()` vs `results()`)
- Incorrect API usage (e.g., `PropertyType.Real` vs `PropertyType.Float`)
- Misunderstanding of CatalogDef structure

**Fix**:
- Corrected all method names and API usage
- **Fixed by**: User (commit a58f62e "Cleaning up Claude's mess")

**Common Fixes**:
- `PropertyType.Real` → `PropertyType.Float`
- `upsertedCount()` → `successCount()`
- `aspects()` → `results()` or `content()`
- `entityIds()` → `content()`
- `catalogIds()` → `content()`
- `AspectDef.id()` → `AspectDef.globalId()`

---

### 4. WebFlux/Servlet Stack Conflict ✅ FIXED

**Error**: `No primary or single unique constructor found for interface org.springframework.http.server.reactive.ServerHttpRequest`

**Root Cause**:
- Integration tests had BOTH `spring-boot-starter-web` (servlet/Tomcat) and `spring-boot-starter-webflux` (reactive/Netty)
- When both present, Spring Boot defaults to servlet stack
- Application uses WebFlux with reactive types (Mono, Flux, ServerWebExchange)
- Servlet container (Tomcat) started instead of Netty
- GlobalExceptionHandler uses ServerWebExchange which servlet container can't inject

**Fix**:
- Removed `spring-boot-starter-web` from integration-tests dependencies
- **Fixed by**: Claude (commit 8e9eaad)

**File**: `integration-tests/build.gradle.kts`

**Before**:
```kotlin
integrationImplementation(libs.spring.boot.starter.web)      // Servlet/Tomcat
integrationImplementation(libs.spring.boot.starter.webflux)  // Reactive/Netty
```

**After**:
```kotlin
integrationImplementation(libs.spring.boot.starter.webflux)  // Reactive/Netty only
```

**Verification**: Application now shows `o.s.b.w.e.netty.GracefulShutdown` instead of `o.s.b.w.e.tomcat.GracefulShutdown`

---

### 5. SQLite Database Schema Not Found ✅ FIXED

**Error**: `[SQLITE_ERROR] SQL error or missing database (no such table: entity)`

**Root Cause**:
- Test created temp file database and initialized schema
- Spring Boot application used separate in-memory database (`jdbc:sqlite::memory:`)
- Two different databases: test's temp file had schema, app's in-memory did not

**Fix**:
- Added `@DynamicPropertySource` to override Spring's datasource URL
- Both test and app now use same temp file database
- **Fixed by**: Claude (commit df6a7ed)

**File**: `integration-tests/src/integration/java/net/netbeing/cheap/integrationtests/base/SqliteRestIntegrationTest.java`

**Added**:
```java
@DynamicPropertySource
static void configureDatasource(DynamicPropertyRegistry registry)
{
    // Configure Spring to use the test's temporary database
    registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + tempDbPath.toAbsolutePath());
}
```

**Verification**: Catalog creation now succeeds with proper database tables

---

## Remaining Issues

### 6. CatalogDefDeserializer NullPointerException ⚠️ REMAINING

**Error**: `NullPointerException: Cannot invoke "String.hashCode()" because "<local6>" is null`

**Location**: `cheap-json/src/main/java/net/netbeing/cheap/json/jackson/deserialize/CatalogDefDeserializer.java:82`

**Root Cause**:
- `p.currentName()` returns null during deserialization
- Switch statement on line 82 tries to hash null `fieldName`
- Likely occurs with empty or certain CatalogDef structures

**Status**:
- **Bug in main codebase** (cheap-json module)
- Not a test issue
- Catalog creation succeeds, retrieval fails during JSON deserialization

**Observed Behavior**:
```
INFO: Successfully created catalog with ID: 44d00724-7f51-435f-b9fe-d026ae338133
INFO: Received request to get catalog 44d00724-7f51-435f-b9fe-d026ae338133
ERROR: NullPointerException at CatalogDefDeserializer.java:82
```

**Recommendation**:
- Add null check for `fieldName` before switch statement
- Or investigate why `p.currentName()` returns null
- May need to handle empty CatalogDef (no aspectDefs, no hierarchyDefs) specially

---

## Test Execution Progress

### Current Status

**Compilations**: ✅ All tests compile successfully

**Runtime**:
- **SQLite**: Catalog creation succeeds, retrieval fails (NullPointerException in deserializer)
- **PostgreSQL**: Not yet tested (likely needs similar datasource fix)
- **MariaDB**: Not yet tested (likely needs datasource configuration)

### Next Steps

1. **Fix CatalogDefDeserializer** (main codebase bug)
   - Add null check or investigate root cause
   - This is blocking all integration tests

2. **Apply datasource fixes to PostgreSQL and MariaDB tests**
   - Check if they need similar `@DynamicPropertySource` configuration
   - PostgreSQL uses embedded instance on port 5433 (may already work)
   - MariaDB uses MariaDB4j embedded instance (may need configuration)

3. **Run full test suite** once deserializer is fixed
   - Test all 25 test methods across 3 databases
   - Verify custom table mapping works
   - Check error handling tests

---

## Commits Made

1. `9c7655f` - Fix YAML syntax errors in integration test configuration files
2. `8e9eaad` - Remove conflicting servlet dependency from integration tests
3. `df6a7ed` - Fix SQLite integration tests to use same database for test and app

**Note**: Commits b35716a and 15d4abf were reverted (attempted to switch cheap-rest to servlet MVC, but app is designed for WebFlux)

---

## Summary

**Fixed Issues**: 5 (SpringBoot config, YAML syntax, API usage, WebFlux/Servlet conflict, SQLite database)

**Remaining Issues**: 1 (CatalogDefDeserializer NullPointerException - main codebase bug)

**Test Status**: Compiling successfully, catalog creation works, retrieval blocked by deserializer bug

**Impact**: Integration tests are very close to working. Only one bug in the main codebase (cheap-json module) is preventing full test execution.
