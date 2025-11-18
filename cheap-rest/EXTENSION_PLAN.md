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

**Test Resources:**
Create JSON files in `cheap-rest/src/test/resources/http-tests/hierarchy/`:
- `add-entity-ids-request.json`
- `remove-entity-ids-request.json`
- `add-directory-entries-request.json`
- `remove-directory-entries-by-names-request.json`
- `remove-directory-entries-by-ids-request.json`
- `add-tree-nodes-request.json`
- `remove-tree-nodes-request.json`

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
