# Cheap-REST-Client Extension Plan: Hierarchy Mutation Methods

## Overview

This document outlines the plan to extend the cheap-rest-client module with new client methods for the hierarchy mutation REST API endpoints added in cheap-rest. These methods will support add/remove operations for Entity Sets, Entity Lists, Entity Directories, and Entity Trees.

## Design Decisions

Based on the cheap-rest API design and existing client patterns:

1. **Consistent Interface**: Follow existing patterns in CheapRestClient for method naming and parameters
2. **Type Safety**: Use strongly-typed request/response DTOs from cheap-json module
3. **Error Handling**: Leverage existing exception hierarchy (CheapRestNotFoundException, CheapRestBadRequestException)
4. **HTTP Verbs**: Use POST for add operations and DELETE for remove operations via WebClient
5. **Blocking API**: Maintain consistency with existing blocking methods (`.block()` on Mono)
6. **Scope**: This plan covers cheap-rest-client only, assuming cheap-rest endpoints are already implemented
7. **Testing**: Comprehensive unit tests with MockWebServer and integration tests with real service

## New Client Methods

### 1. Entity List/Set Operations

**Add Entity IDs to List/Set**
```java
/**
 * Adds entity IDs to an EntityList or EntitySet hierarchy.
 *
 * @param catalogId the catalog ID
 * @param hierarchyName the hierarchy name
 * @param entityIds the list of entity IDs to add
 * @return the operation response with success status and count
 * @throws CheapRestNotFoundException if catalog or hierarchy not found
 * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
 */
EntityIdsOperationResponse addEntityIds(
    @NotNull UUID catalogId,
    @NotNull String hierarchyName,
    @NotNull List<UUID> entityIds
);
```

**Remove Entity IDs from List/Set**
```java
/**
 * Removes entity IDs from an EntityList or EntitySet hierarchy.
 *
 * @param catalogId the catalog ID
 * @param hierarchyName the hierarchy name
 * @param entityIds the list of entity IDs to remove
 * @return the operation response with success status and count
 * @throws CheapRestNotFoundException if catalog or hierarchy not found
 * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
 */
EntityIdsOperationResponse removeEntityIds(
    @NotNull UUID catalogId,
    @NotNull String hierarchyName,
    @NotNull List<UUID> entityIds
);
```

**Implementation Details:**
- **Endpoint**: `/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entities`
- **HTTP Method**: POST for add, DELETE for remove
- **Request Body**: `AddEntityIdsRequest` or `RemoveEntityIdsRequest`
- **Response Type**: `EntityIdsOperationResponse`

### 2. Entity Directory Operations

**Add Directory Entries**
```java
/**
 * Adds entries to an EntityDirectory hierarchy.
 *
 * @param catalogId the catalog ID
 * @param hierarchyName the hierarchy name
 * @param entries map of entry names to entity IDs
 * @return the operation response with success status and count
 * @throws CheapRestNotFoundException if catalog or hierarchy not found
 * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
 */
DirectoryOperationResponse addDirectoryEntries(
    @NotNull UUID catalogId,
    @NotNull String hierarchyName,
    @NotNull Map<String, UUID> entries
);
```

**Remove Directory Entries by Names**
```java
/**
 * Removes directory entries by their names.
 *
 * @param catalogId the catalog ID
 * @param hierarchyName the hierarchy name
 * @param names the list of entry names to remove
 * @return the operation response with success status and count
 * @throws CheapRestNotFoundException if catalog or hierarchy not found
 * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
 */
DirectoryOperationResponse removeDirectoryEntriesByNames(
    @NotNull UUID catalogId,
    @NotNull String hierarchyName,
    @NotNull List<String> names
);
```

**Remove Directory Entries by Entity IDs**
```java
/**
 * Removes directory entries by their entity IDs (removes all entries pointing to these IDs).
 *
 * @param catalogId the catalog ID
 * @param hierarchyName the hierarchy name
 * @param entityIds the list of entity IDs to remove
 * @return the operation response with success status and count
 * @throws CheapRestNotFoundException if catalog or hierarchy not found
 * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
 */
DirectoryOperationResponse removeDirectoryEntriesByEntityIds(
    @NotNull UUID catalogId,
    @NotNull String hierarchyName,
    @NotNull List<UUID> entityIds
);
```

**Implementation Details:**
- **Endpoint**: `/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entries`
- **HTTP Method**: POST for add, DELETE for remove
- **Request Body**: `AddDirectoryEntriesRequest` or `RemoveDirectoryEntriesRequest`
- **Response Type**: `DirectoryOperationResponse`
- **Note**: Remove requests must specify EITHER names OR entity IDs, not both

### 3. Entity Tree Operations

**Add Tree Nodes**
```java
/**
 * Adds nodes to an EntityTree hierarchy under a specified parent.
 *
 * @param catalogId the catalog ID
 * @param hierarchyName the hierarchy name
 * @param parentNodeId the parent node ID (null for root-level nodes)
 * @param childEntityIds map of child entity IDs to their names
 * @return the operation response with success status and count
 * @throws CheapRestNotFoundException if catalog, hierarchy, or parent node not found
 * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
 */
TreeOperationResponse addTreeNodes(
    @NotNull UUID catalogId,
    @NotNull String hierarchyName,
    UUID parentNodeId,
    @NotNull Map<UUID, String> childEntityIds
);
```

**Remove Tree Nodes**
```java
/**
 * Removes nodes from an EntityTree hierarchy (cascade deletes all descendants).
 *
 * @param catalogId the catalog ID
 * @param hierarchyName the hierarchy name
 * @param nodeIds the list of node IDs to remove
 * @return the operation response with success status and count
 * @throws CheapRestNotFoundException if catalog or hierarchy not found
 * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
 */
TreeOperationResponse removeTreeNodes(
    @NotNull UUID catalogId,
    @NotNull String hierarchyName,
    @NotNull List<UUID> nodeIds
);
```

**Implementation Details:**
- **Endpoint**: `/api/catalog/{catalogId}/hierarchies/{hierarchyName}/nodes`
- **HTTP Method**: POST for add, DELETE for remove
- **Request Body**: `AddTreeNodesRequest` or `RemoveTreeNodesRequest`
- **Response Type**: `TreeOperationResponse`
- **Behavior**: Remove operations always cascade delete all descendants

## Implementation Pattern

All methods will follow this pattern in `CheapRestClientImpl`:

```java
@Override
public EntityIdsOperationResponse addEntityIds(
    @NotNull UUID catalogId,
    @NotNull String hierarchyName,
    @NotNull List<UUID> entityIds)
{
    AddEntityIdsRequest request = new AddEntityIdsRequest(entityIds);

    return webClient.post()
        .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entities",
            catalogId, hierarchyName)
        .bodyValue(request)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
        .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
        .bodyToMono(EntityIdsOperationResponse.class)
        .block();
}

@Override
public EntityIdsOperationResponse removeEntityIds(
    @NotNull UUID catalogId,
    @NotNull String hierarchyName,
    @NotNull List<UUID> entityIds)
{
    RemoveEntityIdsRequest request = new RemoveEntityIdsRequest(entityIds);

    // Note: DELETE with body requires special handling
    return webClient.method(HttpMethod.DELETE)
        .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entities",
            catalogId, hierarchyName)
        .bodyValue(request)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
        .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
        .bodyToMono(EntityIdsOperationResponse.class)
        .block();
}
```

## Testing Strategy

### Unit Tests with MockWebServer

**Test File**: `CheapRestClientImplTest.java`

Create comprehensive unit tests for each new method:

#### Success Scenarios
- Add entity IDs to list/set - verify correct request and response
- Remove entity IDs from list/set - verify correct request and response
- Add directory entries - verify map serialization
- Remove directory entries by names - verify list serialization
- Remove directory entries by entity IDs - verify list serialization
- Add tree nodes with parent - verify parent ID handling
- Add tree nodes at root level (null parent) - verify null handling
- Remove tree nodes - verify cascade behavior understanding

#### Error Scenarios
- Catalog not found (404) - verify CheapRestNotFoundException thrown
- Hierarchy not found (404) - verify CheapRestNotFoundException thrown
- Wrong hierarchy type (400) - verify CheapRestBadRequestException thrown
- Empty entity IDs list (400) - verify CheapRestBadRequestException thrown
- Empty directory entries map (400) - verify CheapRestBadRequestException thrown
- Both names and IDs in directory remove (400) - verify server rejects
- Neither names nor IDs in directory remove (400) - verify server rejects
- Parent node not found for tree add (404) - verify CheapRestNotFoundException thrown
- Server error (500) - verify CheapRestServerException thrown

#### MockWebServer Test Pattern
```java
@Test
void testAddEntityIds_Success() {
    UUID catalogId = UUID.randomUUID();
    String hierarchyName = "test-list";
    List<UUID> entityIds = List.of(UUID.randomUUID(), UUID.randomUUID());

    // Mock server response
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody("""
            {
                "success": true,
                "entitiesProcessed": 2,
                "message": "Added 2 entity IDs to hierarchy"
            }
            """)
        .addHeader("Content-Type", "application/json"));

    // Call client method
    EntityIdsOperationResponse response = client.addEntityIds(
        catalogId, hierarchyName, entityIds);

    // Verify response
    assertTrue(response.success());
    assertEquals(2, response.entitiesProcessed());

    // Verify request sent to server
    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().contains("/entities"));

    // Verify request body
    String requestBody = request.getBody().readUtf8();
    assertTrue(requestBody.contains("entityIds"));
}

@Test
void testRemoveEntityIds_HierarchyNotFound() {
    UUID catalogId = UUID.randomUUID();
    String hierarchyName = "nonexistent";
    List<UUID> entityIds = List.of(UUID.randomUUID());

    // Mock 404 response
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(404)
        .setBody("Hierarchy not found: nonexistent"));

    // Verify exception thrown
    assertThrows(CheapRestNotFoundException.class, () ->
        client.removeEntityIds(catalogId, hierarchyName, entityIds)
    );
}
```

### Test Resources

If needed, create JSON test files in `cheap-rest-client/src/test/resources/`:
- `add-entity-ids-response.json` - Sample success response
- `directory-operation-response.json` - Sample directory response
- `tree-operation-response.json` - Sample tree response
- `error-response-404.json` - Sample 404 error
- `error-response-400.json` - Sample 400 error

## Error Scenarios to Test

### Client-Side Validation
- Null catalog ID → NullPointerException or validation error
- Null hierarchy name → NullPointerException or validation error
- Null entity IDs list → NullPointerException or validation error
- Empty entity IDs list → May succeed with 0 processed (check server behavior)

### Server-Side Errors (via MockWebServer)
- 404 Catalog not found
- 404 Hierarchy not found
- 404 Parent node not found (tree operations)
- 400 Wrong hierarchy type
- 400 Invalid request format
- 400 Both names and entity IDs (directory remove)
- 400 Neither names nor entity IDs (directory remove)
- 500 Internal server error

## Notes and Considerations

### Assumptions
- cheap-rest endpoints are fully implemented and tested
- DTOs in cheap-json module are complete and correct
- Existing error handling in CheapRestClientImpl handles all HTTP status codes appropriately
- WebClient configuration supports DELETE with request body

### HTTP DELETE with Body
Note: HTTP DELETE with request body is non-standard but supported by Spring WebClient:
```java
// Use .method(HttpMethod.DELETE) instead of .delete()
webClient.method(HttpMethod.DELETE)
    .uri(...)
    .bodyValue(request)
    .retrieve()
```

### Thread Safety
- CheapRestClient implementation is thread-safe (WebClient is thread-safe)
- All methods are blocking (synchronous) via `.block()`
- Consider adding async variants in future (returning `Mono<T>`)

### Performance Considerations
- Bulk operations are more efficient than multiple single calls
- WebClient connection pooling handles concurrent requests
- Consider adding batch timeout configuration for large operations

### Future Enhancements (Out of Scope)
- Async/reactive method variants (e.g., `Mono<EntityIdsOperationResponse> addEntityIdsAsync(...)`)
- Retry logic for transient failures
- Circuit breaker pattern for resilience
- Request/response logging interceptor
- Metrics collection for client operations
- Builder pattern for complex requests
- Streaming support for very large operations

### Documentation Updates
After implementation:
- Update `cheap-rest-client/README.md` with new method examples
- Add usage examples for each hierarchy type
- Document error handling patterns
- Update Javadoc with detailed examples

## Implementation Checklist

### Phase 1: Interface and DTOs
- [ ] Add new methods to `CheapRestClient` interface
- [ ] Verify all required DTOs exist in cheap-json module
- [ ] Add comprehensive Javadoc to all new methods

### Phase 2: Implementation
- [ ] Implement `addEntityIds` and `removeEntityIds` in `CheapRestClientImpl`
- [ ] Implement `addDirectoryEntries` methods in `CheapRestClientImpl`
- [ ] Implement `removeDirectoryEntriesByNames` in `CheapRestClientImpl`
- [ ] Implement `removeDirectoryEntriesByEntityIds` in `CheapRestClientImpl`
- [ ] Implement `addTreeNodes` in `CheapRestClientImpl`
- [ ] Implement `removeTreeNodes` in `CheapRestClientImpl`
- [ ] Handle DELETE with body using `.method(HttpMethod.DELETE)`

### Phase 3: Unit Tests
- [ ] Write MockWebServer tests for all add operations (success cases)
- [ ] Write MockWebServer tests for all remove operations (success cases)
- [ ] Write MockWebServer tests for 404 errors (all operations)
- [ ] Write MockWebServer tests for 400 errors (all operations)
- [ ] Write MockWebServer tests for 500 errors (representative sample)
- [ ] Verify request serialization in tests
- [ ] Verify response deserialization in tests

### Phase 4: Documentation
- [ ] Update README.md with usage examples
- [ ] Update CLAUDE.md if needed
- [ ] Add code examples for each operation type
- [ ] Document error handling patterns
- [ ] Add troubleshooting section if needed

## References

- cheap-rest EXTENSION_PLAN.md (server-side implementation)
- cheap-rest-client CLAUDE.md (module development guidelines)
- cheap-rest-client README.md (usage documentation)
- cheap-json DTO classes (request/response objects)
- Spring WebClient documentation (HTTP client)
- MockWebServer documentation (testing)
