# Integration Test Failures and Issues

This document tracks compilation errors and test failures discovered during Phase 3 and 4 implementation.

## Phase 3: REST Client Integration Tests

### Compilation Errors Found

**Date**: 2025-11-08

**Status**: Identified - Fixes Pending

#### 1. PropertyType.Real Does Not Exist

**Location**: All three REST client test classes

**Error**:
```
error: cannot find symbol
  PropertyType.Real
              ^
```

**Issue**: Used `PropertyType.Real` but the correct type is `PropertyType.Float`

**Files Affected**:
- `PostgresRestClientIntegrationTest.java`
- `SqliteRestClientIntegrationTest.java`
- `MariaDbRestClientIntegrationTest.java`

**Fix**: Replace `PropertyType.Real` with `PropertyType.Float`

---

#### 2. Incorrect DTO Method Names

**Location**: All three REST client test classes

**Errors**:
```
error: cannot find symbol
  upsertedCount()
  aspects()
  entityIds()
  entries()
```

**Issue**: Used incorrect method names on DTO response objects. Java records use the field name as the accessor method.

**Actual DTO Field Names**:
- `UpsertAspectsResponse`: `successCount` (not `upsertedCount()`)
- `AspectQueryResponse`: `results` (not `aspects()`)
- `EntityListResponse`: `content` (not `entityIds()`)
- `EntityDirectoryResponse`: `content` (not `entries()`)
- `AspectMapResponse`: `content` (not `aspects()`)

**Files Affected**:
- `PostgresRestClientIntegrationTest.java`
- `SqliteRestClientIntegrationTest.java`
- `MariaDbRestClientIntegrationTest.java`

**Fix**: Use correct field names: `successCount`, `results`, `content`

---

#### 3. Incorrect CatalogDef API

**Location**: All three REST client test classes

**Error**:
```
error: no suitable method found for createCatalogDef(String,String,<null>)
```

**Issue**: Attempted to create `CatalogDef` with name and description, but `CatalogDef` doesn't have those fields. `CatalogDef` only contains hierarchy and aspect definitions.

**Available Factory Methods**:
```java
public @NotNull CatalogDef createCatalogDef()
public @NotNull CatalogDef createCatalogDef(@NotNull CatalogDef other)
public @NotNull CatalogDef createCatalogDef(Iterable<HierarchyDef> hierarchyDefs, Iterable<AspectDef> aspectDefs)
```

**Files Affected**:
- `PostgresRestClientIntegrationTest.java`
- `SqliteRestClientIntegrationTest.java`
- `MariaDbRestClientIntegrationTest.java`

**Fix**: Use `factory.createCatalogDef()` to create empty catalog definitions

---

#### 4. CatalogDef Has No name() Method

**Location**: All three REST client test classes

**Error**:
```
error: cannot find symbol
  retrieved.name()
           ^
```

**Issue**: `CatalogDef` interface doesn't have `name()` or `description()` methods. These properties don't exist on CatalogDef.

**Files Affected**:
- `PostgresRestClientIntegrationTest.java`
- `SqliteRestClientIntegrationTest.java`
- `MariaDbRestClientIntegrationTest.java`

**Fix**: Remove assertions on name/description - they aren't part of `CatalogDef`

---

### Summary of Required Fixes

1. Replace all `PropertyType.Real` → `PropertyType.Float` (3 files, multiple occurrences)
2. Replace `upsertedCount()` → `successCount` (3 files, 1 occurrence each)
3. Replace `queryResponse.aspects()` → `queryResponse.results()` (3 files, 3 occurrences each)
4. Replace `page1.entityIds()` → `page1.content()` (3 files, 3 occurrences each)
5. Replace `response.entries()` → `response.content()` (3 files, 1 occurrence each)
6. Replace `aspectMap.aspects()` → `aspectMap.content()` (3 files, 3 occurrences each)
7. Replace `factory.createCatalogDef("name", "desc", null)` → `factory.createCatalogDef()` (3 files, 8 occurrences each)
8. Remove assertions on `retrieved.name()` and `retrieved.description()` (3 files, 2 occurrences each)

---

### Total Errors (First Pass)

**101 compilation errors** across 3 test files

---

### Additional Errors Found After Partial Fixes

**Date**: 2025-11-08 (second pass)

**Status**: 22 errors remaining after first round of fixes

#### 9. CatalogListResponse Has content, Not catalogIds

**Error**:
```
error: cannot find symbol
  catalogIds()
```

**Issue**: `CatalogListResponse` has `content`, not `catalogIds()`

**Fix**: Replace `catalogIds()` → `content()`

---

#### 10. AspectDef.id() Does Not Exist

**Error**:
```
error: cannot find symbol
  retrievedOrderItem.id()
```

**Issue**: `AspectDef` extends `Entity` which has `globalId()`, not `id()`

**Fix**: Replace `.id()` → `.globalId()`

---

#### 11. successCount is Method, Not Field

**Error**:
```
error: successCount has private access in UpsertAspectsResponse
```

**Issue**: Removed `()` from record accessor - it should be `successCount()` not `successCount`

**Fix**: Replace `successCount` → `successCount()`

---

#### 12. AspectQueryResponse Returns Aspect Objects, Not Maps

**Error**:
```
error: incompatible types: Aspect cannot be converted to Map<String,Object>
```

**Issue**: `AspectQueryResponse.results()` returns `Map<UUID, Map<String, Aspect>>`, where the inner value is an `Aspect` object, not `Map<String, Object>`

**Fix**: Need to work with `Aspect` interface to extract property values, or adjust test expectations

---

### Total Errors (Second Pass)

**22 compilation errors** remaining across 3 test files

---

## Phase 3 Status Summary

**Initial Test Implementation**: Complete
**Compilation Status**: **FAILING** - 22 errors remaining
**Root Cause**: Incomplete understanding of DTOs and model interfaces
**Impact**: Phase 3 tests cannot run until compilation errors are resolved
**Recommended Action**: Review actual DTO/model interfaces more carefully and create corrected tests

---

## Phase 4: Cross-Database Consistency Tests

**Status**: Implementation deferred - proceeding per user request to continue without stopping

---

## Notes

- These are not actual test failures but compilation errors due to incorrect API usage
- Initial batch fixes reduced errors from 101 to 22
- Remaining errors indicate need for deeper API understanding
- The test logic and structure appear sound; only API signatures need correction
- No changes to main codebase are required; all fixes are in test code only
- **Recommendation**: May need to refactor tests with better understanding of Aspect interface
