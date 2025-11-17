# Cheap-REST Extension Plan: Hierarchy Mutation Endpoints

## Overview

This document outlines the plan to extend the cheap-rest module with new REST API endpoints for mutating hierarchy contents. These endpoints will support add/remove operations for Entity Sets, Entity Lists, Entity Directories, and Entity Trees.

## Design Decisions

Based on requirements and architectural consistency:

1. **Unified Endpoints**: Use the same endpoint paths for similar hierarchy types (e.g., EntitySet and EntityList both use `/entities`)
2. **RESTful HTTP Verbs**: Use POST for add operations and DELETE for remove operations on the same endpoint paths
3. **Bulk Operations**: All operations support multiple entities/entries per request via arrays/maps in the request body
4. **Tree Cascade Deletion**: Removing a tree node always removes all descendants (cascade delete behavior)
5. **Scope**: This plan covers cheap-rest only, assuming cheap-core interfaces already provide the necessary operations
6. **Consistency**: Follow existing patterns in HierarchyController for endpoint structure and error handling

## New API Endpoints

### 1. Entity List/Set Operations

**Add Entity IDs**
- **Method**: POST
- **Path**: `/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entities`
- **Supported Types**: EntityList, EntitySet
- **Request Body**: `AddEntityIdsRequest`
- **Response**: `EntityIdsOperationResponse`
- **Status Codes**: 200 OK, 404 Not Found (catalog/hierarchy), 400 Bad Request (wrong hierarchy type)

**Remove Entity IDs**
- **Method**: DELETE
- **Path**: `/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entities`
- **Supported Types**: EntityList, EntitySet
- **Request Body**: `RemoveEntityIdsRequest`
- **Response**: `EntityIdsOperationResponse`
- **Status Codes**: 200 OK, 404 Not Found, 400 Bad Request

### 2. Entity Directory Operations

**Add Directory Entries**
- **Method**: POST
- **Path**: `/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entries`
- **Supported Types**: EntityDirectory
- **Request Body**: `AddDirectoryEntriesRequest`
- **Response**: `DirectoryOperationResponse`
- **Status Codes**: 200 OK, 404 Not Found, 400 Bad Request (wrong hierarchy type)

**Remove Directory Entries**
- **Method**: DELETE
- **Path**: `/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entries`
- **Supported Types**: EntityDirectory
- **Request Body**: `RemoveDirectoryEntriesRequest`
- **Response**: `DirectoryOperationResponse`
- **Status Codes**: 200 OK, 404 Not Found, 400 Bad Request
- **Note**: Request must specify removal either by entity IDs OR by names, not both

### 3. Entity Tree Operations

**Add Tree Nodes**
- **Method**: POST
- **Path**: `/api/catalog/{catalogId}/hierarchies/{hierarchyName}/nodes`
- **Supported Types**: EntityTree
- **Request Body**: `AddTreeNodesRequest`
- **Response**: `TreeOperationResponse`
- **Status Codes**: 200 OK, 404 Not Found (catalog/hierarchy/parent node), 400 Bad Request

**Remove Tree Nodes**
- **Method**: DELETE
- **Path**: `/api/catalog/{catalogId}/hierarchies/{hierarchyName}/nodes`
- **Supported Types**: EntityTree
- **Request Body**: `RemoveTreeNodesRequest`
- **Response**: `TreeOperationResponse`
- **Status Codes**: 200 OK, 404 Not Found, 400 Bad Request
- **Behavior**: Always removes all descendants (cascade delete)

## Data Transfer Objects (DTOs)

All DTOs will be created in the `cheap-json` module at:
`cheap-json/src/main/java/net/netbeing/cheap/json/dto/`

### Request DTOs

#### AddEntityIdsRequest
```java
package net.netbeing.cheap.json.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to add entity IDs to an EntityList or EntitySet hierarchy.
 *
 * @param entityIds List of entity IDs to add (must not be null or empty)
 */
public record AddEntityIdsRequest(
    List<UUID> entityIds
) {
}
```

#### RemoveEntityIdsRequest
```java
package net.netbeing.cheap.json.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to remove entity IDs from an EntityList or EntitySet hierarchy.
 *
 * @param entityIds List of entity IDs to remove (must not be null or empty)
 */
public record RemoveEntityIdsRequest(
    List<UUID> entityIds
) {
}
```

#### AddDirectoryEntriesRequest
```java
package net.netbeing.cheap.json.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Request to add (name, entityId) pairs to an EntityDirectory hierarchy.
 *
 * @param entries Map of name -> entity ID pairs to add (must not be null or empty)
 */
public record AddDirectoryEntriesRequest(
    Map<String, UUID> entries
) {
}
```

#### RemoveDirectoryEntriesRequest
```java
package net.netbeing.cheap.json.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to remove entries from an EntityDirectory hierarchy.
 * Must specify EITHER names OR entityIds, not both.
 *
 * @param names List of names to remove (null if removing by entity IDs)
 * @param entityIds List of entity IDs to remove (null if removing by names)
 */
public record RemoveDirectoryEntriesRequest(
    List<String> names,
    List<UUID> entityIds
) {
}
```

#### AddTreeNodesRequest
```java
package net.netbeing.cheap.json.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Request to add child nodes to an EntityTree hierarchy under a specific parent.
 *
 * @param parentPath Path to the parent node (e.g., "/root/folder1")
 * @param nodes Map of child name -> entity ID pairs to add (must not be null or empty)
 */
public record AddTreeNodesRequest(
    String parentPath,
    Map<String, UUID> nodes
) {
}
```

#### RemoveTreeNodesRequest
```java
package net.netbeing.cheap.json.dto;

import java.util.List;

/**
 * Request to remove nodes from an EntityTree hierarchy.
 * Removal always cascades to remove all descendants.
 *
 * @param paths List of node paths to remove (must not be null or empty)
 */
public record RemoveTreeNodesRequest(
    List<String> paths
) {
}
```

### Response DTOs

#### EntityIdsOperationResponse
```java
package net.netbeing.cheap.json.dto;

import java.util.UUID;

/**
 * Response for entity ID add/remove operations on EntityList or EntitySet hierarchies.
 *
 * @param catalogId ID of the catalog
 * @param hierarchyName Name of the hierarchy
 * @param operation Operation performed ("add" or "remove")
 * @param count Number of entity IDs added or removed
 * @param message Success message
 */
public record EntityIdsOperationResponse(
    UUID catalogId,
    String hierarchyName,
    String operation,
    int count,
    String message
) {
}
```

#### DirectoryOperationResponse
```java
package net.netbeing.cheap.json.dto;

import java.util.UUID;

/**
 * Response for directory entry add/remove operations on EntityDirectory hierarchies.
 *
 * @param catalogId ID of the catalog
 * @param hierarchyName Name of the hierarchy
 * @param operation Operation performed ("add" or "remove")
 * @param count Number of entries added or removed
 * @param message Success message
 */
public record DirectoryOperationResponse(
    UUID catalogId,
    String hierarchyName,
    String operation,
    int count,
    String message
) {
}
```

#### TreeOperationResponse
```java
package net.netbeing.cheap.json.dto;

import java.util.UUID;

/**
 * Response for tree node add/remove operations on EntityTree hierarchies.
 *
 * @param catalogId ID of the catalog
 * @param hierarchyName Name of the hierarchy
 * @param operation Operation performed ("add" or "remove")
 * @param nodesAffected Number of nodes affected (for remove, includes cascade deleted nodes)
 * @param message Success message
 */
public record TreeOperationResponse(
    UUID catalogId,
    String hierarchyName,
    String operation,
    int nodesAffected,
    String message
) {
}
```

## Implementation Plan by Layer

### Layer 1: DTOs (cheap-json module)

**Files to Create:**
1. `cheap-json/src/main/java/net/netbeing/cheap/json/dto/AddEntityIdsRequest.java`
2. `cheap-json/src/main/java/net/netbeing/cheap/json/dto/RemoveEntityIdsRequest.java`
3. `cheap-json/src/main/java/net/netbeing/cheap/json/dto/AddDirectoryEntriesRequest.java`
4. `cheap-json/src/main/java/net/netbeing/cheap/json/dto/RemoveDirectoryEntriesRequest.java`
5. `cheap-json/src/main/java/net/netbeing/cheap/json/dto/AddTreeNodesRequest.java`
6. `cheap-json/src/main/java/net/netbeing/cheap/json/dto/RemoveTreeNodesRequest.java`
7. `cheap-json/src/main/java/net/netbeing/cheap/json/dto/EntityIdsOperationResponse.java`
8. `cheap-json/src/main/java/net/netbeing/cheap/json/dto/DirectoryOperationResponse.java`
9. `cheap-json/src/main/java/net/netbeing/cheap/json/dto/TreeOperationResponse.java`

**Validation Requirements:**
- Add `@NotNull` and `@NotEmpty` annotations where appropriate
- For `RemoveDirectoryEntriesRequest`: Add validation to ensure exactly one of `names` or `entityIds` is non-null

### Layer 2: Service Layer (HierarchyService)

**File to Modify:**
`cheap-rest/src/main/java/net/netbeing/cheap/rest/service/HierarchyService.java`

**New Methods to Add:**

```java
// Entity List/Set Operations
@Transactional
public int addEntityIds(UUID catalogId, String hierarchyName, List<UUID> entityIds);

@Transactional
public int removeEntityIds(UUID catalogId, String hierarchyName, List<UUID> entityIds);

// Entity Directory Operations
@Transactional
public int addDirectoryEntries(UUID catalogId, String hierarchyName, Map<String, UUID> entries);

@Transactional
public int removeDirectoryEntriesByNames(UUID catalogId, String hierarchyName, List<String> names);

@Transactional
public int removeDirectoryEntriesByIds(UUID catalogId, String hierarchyName, List<UUID> entityIds);

// Entity Tree Operations
@Transactional
public int addTreeNodes(UUID catalogId, String hierarchyName, String parentPath, Map<String, UUID> nodes);

@Transactional
public int removeTreeNodes(UUID catalogId, String hierarchyName, List<String> paths);
```

**Implementation Notes:**
- Each method should retrieve the hierarchy and validate its type
- Use pattern matching to cast to the correct hierarchy type
- Call the appropriate cheap-core hierarchy interface methods
- Throw `ResourceNotFoundException` if catalog/hierarchy not found
- Throw `ValidationException` if wrong hierarchy type
- Return the count of entities/entries affected

### Layer 3: Reactive Service Layer (ReactiveHierarchyService)

**File to Modify:**
`cheap-rest/src/main/java/net/netbeing/cheap/rest/service/ReactiveHierarchyService.java`

**New Methods to Add:**

```java
// Entity List/Set Operations
public Mono<Integer> addEntityIds(UUID catalogId, String hierarchyName, List<UUID> entityIds) {
    return Mono.fromCallable(() -> hierarchyService.addEntityIds(catalogId, hierarchyName, entityIds))
               .subscribeOn(jdbcScheduler);
}

public Mono<Integer> removeEntityIds(UUID catalogId, String hierarchyName, List<UUID> entityIds) {
    return Mono.fromCallable(() -> hierarchyService.removeEntityIds(catalogId, hierarchyName, entityIds))
               .subscribeOn(jdbcScheduler);
}

// Entity Directory Operations
public Mono<Integer> addDirectoryEntries(UUID catalogId, String hierarchyName, Map<String, UUID> entries) {
    return Mono.fromCallable(() -> hierarchyService.addDirectoryEntries(catalogId, hierarchyName, entries))
               .subscribeOn(jdbcScheduler);
}

public Mono<Integer> removeDirectoryEntriesByNames(UUID catalogId, String hierarchyName, List<String> names) {
    return Mono.fromCallable(() -> hierarchyService.removeDirectoryEntriesByNames(catalogId, hierarchyName, names))
               .subscribeOn(jdbcScheduler);
}

public Mono<Integer> removeDirectoryEntriesByIds(UUID catalogId, String hierarchyName, List<UUID> entityIds) {
    return Mono.fromCallable(() -> hierarchyService.removeDirectoryEntriesByIds(catalogId, hierarchyName, entityIds))
               .subscribeOn(jdbcScheduler);
}

// Entity Tree Operations
public Mono<Integer> addTreeNodes(UUID catalogId, String hierarchyName, String parentPath, Map<String, UUID> nodes) {
    return Mono.fromCallable(() -> hierarchyService.addTreeNodes(catalogId, hierarchyName, parentPath, nodes))
               .subscribeOn(jdbcScheduler);
}

public Mono<Integer> removeTreeNodes(UUID catalogId, String hierarchyName, List<String> paths) {
    return Mono.fromCallable(() -> hierarchyService.removeTreeNodes(catalogId, hierarchyName, paths))
               .subscribeOn(jdbcScheduler);
}
```

### Layer 4: Controller Layer (HierarchyController)

**File to Modify:**
`cheap-rest/src/main/java/net/netbeing/cheap/rest/controller/HierarchyController.java`

**New Endpoint Methods to Add:**

```java
// Entity List/Set Operations
@PostMapping("/{hierarchyName}/entities")
public Mono<EntityIdsOperationResponse> addEntityIds(
    @PathVariable UUID catalogId,
    @PathVariable String hierarchyName,
    @RequestBody AddEntityIdsRequest request) {

    return hierarchyService.addEntityIds(catalogId, hierarchyName, request.entityIds())
        .map(count -> new EntityIdsOperationResponse(
            catalogId,
            hierarchyName,
            "add",
            count,
            String.format("Added %d entity ID(s) to hierarchy '%s'", count, hierarchyName)
        ));
}

@DeleteMapping("/{hierarchyName}/entities")
public Mono<EntityIdsOperationResponse> removeEntityIds(
    @PathVariable UUID catalogId,
    @PathVariable String hierarchyName,
    @RequestBody RemoveEntityIdsRequest request) {

    return hierarchyService.removeEntityIds(catalogId, hierarchyName, request.entityIds())
        .map(count -> new EntityIdsOperationResponse(
            catalogId,
            hierarchyName,
            "remove",
            count,
            String.format("Removed %d entity ID(s) from hierarchy '%s'", count, hierarchyName)
        ));
}

// Entity Directory Operations
@PostMapping("/{hierarchyName}/entries")
public Mono<DirectoryOperationResponse> addDirectoryEntries(
    @PathVariable UUID catalogId,
    @PathVariable String hierarchyName,
    @RequestBody AddDirectoryEntriesRequest request) {

    return hierarchyService.addDirectoryEntries(catalogId, hierarchyName, request.entries())
        .map(count -> new DirectoryOperationResponse(
            catalogId,
            hierarchyName,
            "add",
            count,
            String.format("Added %d entry/entries to directory '%s'", count, hierarchyName)
        ));
}

@DeleteMapping("/{hierarchyName}/entries")
public Mono<DirectoryOperationResponse> removeDirectoryEntries(
    @PathVariable UUID catalogId,
    @PathVariable String hierarchyName,
    @RequestBody RemoveDirectoryEntriesRequest request) {

    // Validate that exactly one of names or entityIds is provided
    if ((request.names() == null || request.names().isEmpty()) ==
        (request.entityIds() == null || request.entityIds().isEmpty())) {
        return Mono.error(new ValidationException(
            "Must specify either 'names' or 'entityIds', but not both or neither"));
    }

    Mono<Integer> operation = request.names() != null && !request.names().isEmpty()
        ? hierarchyService.removeDirectoryEntriesByNames(catalogId, hierarchyName, request.names())
        : hierarchyService.removeDirectoryEntriesByIds(catalogId, hierarchyName, request.entityIds());

    return operation.map(count -> new DirectoryOperationResponse(
        catalogId,
        hierarchyName,
        "remove",
        count,
        String.format("Removed %d entry/entries from directory '%s'", count, hierarchyName)
    ));
}

// Entity Tree Operations
@PostMapping("/{hierarchyName}/nodes")
public Mono<TreeOperationResponse> addTreeNodes(
    @PathVariable UUID catalogId,
    @PathVariable String hierarchyName,
    @RequestBody AddTreeNodesRequest request) {

    return hierarchyService.addTreeNodes(catalogId, hierarchyName, request.parentPath(), request.nodes())
        .map(count -> new TreeOperationResponse(
            catalogId,
            hierarchyName,
            "add",
            count,
            String.format("Added %d node(s) to tree '%s' under path '%s'",
                count, hierarchyName, request.parentPath())
        ));
}

@DeleteMapping("/{hierarchyName}/nodes")
public Mono<TreeOperationResponse> removeTreeNodes(
    @PathVariable UUID catalogId,
    @PathVariable String hierarchyName,
    @RequestBody RemoveTreeNodesRequest request) {

    return hierarchyService.removeTreeNodes(catalogId, hierarchyName, request.paths())
        .map(count -> new TreeOperationResponse(
            catalogId,
            hierarchyName,
            "remove",
            count,
            String.format("Removed %d node(s) from tree '%s' (including descendants)",
                count, hierarchyName)
        ));
}
```

### Layer 5: Service Tests (HierarchyServiceTest)

**File to Modify:**
`cheap-rest/src/test/java/net/netbeing/cheap/rest/service/HierarchyServiceTest.java`

**New Test Methods to Add:**

```java
// Entity List Operations
@Test
void testAddEntityIdsToEntityList()
@Test
void testRemoveEntityIdsFromEntityList()
@Test
void testAddEntityIdsToWrongHierarchyTypeThrows()

// Entity Set Operations
@Test
void testAddEntityIdsToEntitySet()
@Test
void testRemoveEntityIdsFromEntitySet()

// Entity Directory Operations
@Test
void testAddDirectoryEntries()
@Test
void testRemoveDirectoryEntriesByNames()
@Test
void testRemoveDirectoryEntriesByIds()
@Test
void testAddDirectoryEntriesToWrongHierarchyTypeThrows()

// Entity Tree Operations
@Test
void testAddTreeNodes()
@Test
void testRemoveTreeNodesWithCascade()
@Test
void testAddTreeNodesToNonexistentParentThrows()
@Test
void testAddTreeNodesToWrongHierarchyTypeThrows()

// Edge Cases
@Test
void testAddEmptyEntityIdsListThrows()
@Test
void testRemoveNonexistentEntityIds()
@Test
void testAddDuplicateDirectoryEntries()
```

**Test Patterns:**
- Create test hierarchies using existing helper methods
- Call service methods with test data
- Assert return values (counts) are correct
- Verify state changes by querying hierarchy contents
- Test error conditions with `assertThrows()`

### Layer 6: Controller HTTP Tests (HierarchyControllerHttpTest)

**File to Modify:**
`cheap-rest/src/test/java/net/netbeing/cheap/rest/controller/HierarchyControllerHttpTest.java`

**New Test Methods to Add:**

```java
// Entity List/Set Operations
@Test
void testAddEntityIdsToEntityListHttp()
@Test
void testRemoveEntityIdsFromEntityListHttp()
@Test
void testAddEntityIdsToEntitySetHttp()
@Test
void testAddEntityIdsToNonexistentHierarchyReturns404()
@Test
void testAddEntityIdsToWrongHierarchyTypeReturns400()

// Entity Directory Operations
@Test
void testAddDirectoryEntriesHttp()
@Test
void testRemoveDirectoryEntriesByNamesHttp()
@Test
void testRemoveDirectoryEntriesByIdsHttp()
@Test
void testRemoveDirectoryEntriesWithBothNamesAndIdsReturns400()
@Test
void testRemoveDirectoryEntriesWithNeitherNamesNorIdsReturns400()

// Entity Tree Operations
@Test
void testAddTreeNodesHttp()
@Test
void testRemoveTreeNodesHttp()
@Test
void testAddTreeNodesToNonexistentParentReturns404()

// JSON Validation
@Test
void testAddEntityIdsRequestDeserializesCorrectly()
@Test
void testDirectoryOperationResponseSerializesCorrectly()
@Test
void testTreeOperationResponseSerializesCorrectly()
```

**Test Resources:**
Create JSON files in `cheap-rest/src/test/resources/http-tests/hierarchy/`:
- `add-entity-ids-request.json`
- `remove-entity-ids-request.json`
- `add-directory-entries-request.json`
- `remove-directory-entries-by-names-request.json`
- `remove-directory-entries-by-ids-request.json`
- `add-tree-nodes-request.json`
- `remove-tree-nodes-request.json`

**Test Pattern:**
```java
@Test
void testAddEntityIdsToEntityListHttp() {
    // Setup: Create catalog and hierarchy
    UUID catalogId = createTestCatalog();
    String hierarchyName = createTestEntityList(catalogId);

    // Load request JSON
    String requestJson = loadJson("http-tests/hierarchy/add-entity-ids-request.json");

    // Execute request
    webTestClient.post()
        .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entities",
             catalogId, hierarchyName)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestJson)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.catalogId").isEqualTo(catalogId.toString())
        .jsonPath("$.hierarchyName").isEqualTo(hierarchyName)
        .jsonPath("$.operation").isEqualTo("add")
        .jsonPath("$.count").isEqualTo(3)
        .jsonPath("$.message").exists();

    // Verify state change by querying hierarchy
    verifyEntityListContains(catalogId, hierarchyName, expectedEntityIds);
}
```

## Implementation Order

### Phase 1: DTOs and Core Service Methods
1. Create all 9 DTO files in cheap-json module
2. Add service methods to HierarchyService
3. Add reactive wrappers to ReactiveHierarchyService
4. Add comprehensive service tests to HierarchyServiceTest

### Phase 2: Controller and HTTP Tests
1. Add controller endpoint methods to HierarchyController
2. Create JSON test resource files
3. Add comprehensive HTTP tests to HierarchyControllerHttpTest

### Phase 3: Validation and Polish
1. Run all tests and ensure 100% pass rate
2. Verify error handling and edge cases
3. Test with actual REST client (manual or Postman)
4. Update cheap-rest module documentation if needed

## Testing Strategy

### Unit/Integration Tests (Service Layer)
- **Coverage**: All service methods with success and error paths
- **Approach**: Direct service method invocation with SQLite database
- **Focus**: Business logic, validation, transaction behavior
- **Test Data**: Programmatically created hierarchies and entities

### End-to-End Tests (Controller Layer)
- **Coverage**: All HTTP endpoints with various request bodies
- **Approach**: WebTestClient with full Spring Boot context
- **Focus**: HTTP semantics, JSON serialization, status codes
- **Test Data**: JSON files in test resources

### Error Scenarios to Test
- Catalog not found (404)
- Hierarchy not found (404)
- Wrong hierarchy type (400)
- Empty request lists (400)
- Invalid request format (400)
- Parent node not found for tree operations (404)
- Both names and IDs provided for directory removal (400)
- Neither names nor IDs provided for directory removal (400)

## Success Criteria

1. All 9 DTOs created and properly documented
2. All 7 service methods implemented with @Transactional annotations
3. All 7 reactive wrappers implemented
4. All 6 controller endpoints implemented
5. All service tests pass (minimum 15 test methods)
6. All controller HTTP tests pass (minimum 15 test methods)
7. No regressions in existing tests
8. Code follows existing patterns and style conventions
9. All tests use AssertJ assertions for consistency
10. Manual testing confirms all endpoints work as expected

## Notes and Considerations

### Assumptions
- cheap-core interfaces (EntityListHierarchy, EntitySetHierarchy, EntityDirectoryHierarchy, EntityTreeHierarchy) already provide methods for these operations
- The cheap-db-* modules already implement these operations in their persistence layers
- Entity IDs referenced in requests may or may not exist as actual entities (hierarchies store references)

### Future Enhancements (Out of Scope)
- Bulk operations with partial success/failure reporting
- Undo/rollback capabilities
- Audit logging for mutation operations
- Optimistic locking/versioning for concurrent modifications
- Batch operations endpoint for multiple hierarchy mutations

### Performance Considerations
- Bulk operations are more efficient than multiple single operations
- Tree cascade deletes may affect many nodes - consider limits
- Directory operations with large entry counts may need pagination in future

## References

- Existing HierarchyController implementation
- CHEAP core concepts documentation (.claude/docs/core-concepts.md)
- cheap-rest module CLAUDE.md
- Spring WebFlux best practices
