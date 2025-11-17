/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.rest.controller;

import net.netbeing.cheap.model.*;
import net.netbeing.cheap.json.dto.*;
import net.netbeing.cheap.rest.service.ReactiveHierarchyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import net.netbeing.cheap.rest.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for Hierarchy operations.
 * Uses reactive types (Mono) to provide non-blocking HTTP handling.
 */
@RestController
@RequestMapping("/api/catalog/{catalogId}/hierarchies")
public class HierarchyController
{
    private static final Logger logger = LoggerFactory.getLogger(HierarchyController.class);

    private final ReactiveHierarchyService hierarchyService;

    @Value("${cheap.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${cheap.pagination.max-page-size:100}")
    private int maxPageSize;

    public HierarchyController(ReactiveHierarchyService hierarchyService)
    {
        this.hierarchyService = hierarchyService;
    }

    /**
     * Creates a new hierarchy in a catalog.
     *
     * @param catalogId the catalog ID
     * @param request the hierarchy creation request
     * @return Mono emitting the created hierarchy response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateHierarchyResponse> createHierarchy(
        @PathVariable UUID catalogId,
        @RequestBody CreateHierarchyRequest request)
    {
        logger.info("Received request to create hierarchy {} in catalog {}",
            request.hierarchyDef().name(), catalogId);

        return hierarchyService.createHierarchy(catalogId, request.hierarchyDef())
            .map(hierarchyName -> new CreateHierarchyResponse(
                hierarchyName,
                "Hierarchy created successfully"
            ));
    }

    /**
     * Gets hierarchy contents by name reactively.
     * Returns different response types based on hierarchy type.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param page the page number (default: 0) - ignored for EntityTree
     * @param size the page size (default: from config) - ignored for EntityTree
     * @return Mono emitting hierarchy contents with appropriate structure
     */
    @GetMapping("/{hierarchyName}")
    @SuppressWarnings("java:S1452")
    public Mono<?> getHierarchy(
        @PathVariable UUID catalogId,
        @PathVariable String hierarchyName,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size)
    {
        logger.info("Received request to get hierarchy {} from catalog {}", hierarchyName, catalogId);

        return hierarchyService.getHierarchy(catalogId, hierarchyName)
            .flatMap(hierarchy -> switch (hierarchy) {
                case EntityListHierarchy entityList ->
                    handleEntityList(catalogId, hierarchyName, entityList, page, size);
                case EntitySetHierarchy entitySet ->
                    handleEntitySet(catalogId, hierarchyName, entitySet, page, size);
                case EntityDirectoryHierarchy entityDir ->
                    handleEntityDirectory(catalogId, hierarchyName, entityDir, page, size);
                case EntityTreeHierarchy entityTree ->
                    handleEntityTree(catalogId, hierarchyName, entityTree);
                case AspectMapHierarchy aspectMap ->
                    handleAspectMap(catalogId, hierarchyName, aspectMap, page, size);
                default -> Mono.error(new IllegalStateException(
                    "Unknown hierarchy type: " + hierarchy.getClass().getName()
                ));
            });
    }

    private Mono<EntityListResponse> handleEntityList(
        UUID catalogId, String hierarchyName, EntityListHierarchy hierarchy, int page, Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        return Mono.zip(
                hierarchyService.getEntityListContents(hierarchy, page, pageSize),
                hierarchyService.countHierarchyItems(hierarchy)
            )
            .map(tuple -> {
                var content = tuple.getT1();
                long totalElements = tuple.getT2();
                int totalPages = (int) Math.ceil((double) totalElements / pageSize);

                return new EntityListResponse(
                    catalogId,
                    hierarchyName,
                    content,
                    page,
                    pageSize,
                    totalElements,
                    totalPages
                );
            });
    }

    private Mono<EntityListResponse> handleEntitySet(
        UUID catalogId, String hierarchyName, EntitySetHierarchy hierarchy, int page, Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        return Mono.zip(
                hierarchyService.getEntitySetContents(hierarchy, page, pageSize),
                hierarchyService.countHierarchyItems(hierarchy)
            )
            .map(tuple -> {
                var content = tuple.getT1();
                long totalElements = tuple.getT2();
                int totalPages = (int) Math.ceil((double) totalElements / pageSize);

                return new EntityListResponse(
                    catalogId,
                    hierarchyName,
                    content,
                    page,
                    pageSize,
                    totalElements,
                    totalPages
                );
            });
    }

    private Mono<EntityDirectoryResponse> handleEntityDirectory(
        UUID catalogId, String hierarchyName, EntityDirectoryHierarchy hierarchy, int page, Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        return Mono.zip(
                hierarchyService.getEntityDirectoryContents(hierarchy, page, pageSize),
                hierarchyService.countHierarchyItems(hierarchy)
            )
            .map(tuple -> {
                var content = tuple.getT1();
                long totalElements = tuple.getT2();
                int totalPages = (int) Math.ceil((double) totalElements / pageSize);

                return new EntityDirectoryResponse(
                    catalogId,
                    hierarchyName,
                    content,
                    page,
                    pageSize,
                    totalElements,
                    totalPages
                );
            });
    }

    private Mono<EntityTreeResponse> handleEntityTree(
        UUID catalogId, String hierarchyName, EntityTreeHierarchy hierarchy)
    {
        return hierarchyService.getEntityTreeContents(hierarchy)
            .map(root -> new EntityTreeResponse(
                catalogId,
                hierarchyName,
                root
            ));
    }

    private Mono<AspectMapResponse> handleAspectMap(
        UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy, int page, Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        return Mono.zip(
                hierarchyService.getAspectMapContents(hierarchy, page, pageSize),
                hierarchyService.countHierarchyItems(hierarchy)
            )
            .map(tuple -> {
                var content = tuple.getT1();
                long totalElements = tuple.getT2();
                int totalPages = (int) Math.ceil((double) totalElements / pageSize);

                return new AspectMapResponse(
                    catalogId,
                    hierarchyName,
                    hierarchy.aspectDef().name(),
                    content,
                    page,
                    pageSize,
                    totalElements,
                    totalPages
                );
            });
    }

    // ========================================
    // Entity List/Set Mutation Operations
    // ========================================

    /**
     * Adds entity IDs to an EntityList or EntitySet hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param request the request containing entity IDs to add
     * @return Mono emitting the operation response
     */
    @PostMapping("/{hierarchyName}/entities")
    public Mono<EntityIdsOperationResponse> addEntityIds(
        @PathVariable UUID catalogId,
        @PathVariable String hierarchyName,
        @RequestBody AddEntityIdsRequest request)
    {
        logger.info("Received request to add {} entity IDs to hierarchy {} in catalog {}",
            request.entityIds().size(), hierarchyName, catalogId);

        return hierarchyService.addEntityIds(catalogId, hierarchyName, request.entityIds())
            .map(count -> new EntityIdsOperationResponse(
                catalogId,
                hierarchyName,
                "add",
                count,
                String.format("Added %d entity ID(s) to hierarchy '%s'", count, hierarchyName)
            ));
    }

    /**
     * Removes entity IDs from an EntityList or EntitySet hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param request the request containing entity IDs to remove
     * @return Mono emitting the operation response
     */
    @DeleteMapping("/{hierarchyName}/entities")
    public Mono<EntityIdsOperationResponse> removeEntityIds(
        @PathVariable UUID catalogId,
        @PathVariable String hierarchyName,
        @RequestBody RemoveEntityIdsRequest request)
    {
        logger.info("Received request to remove {} entity IDs from hierarchy {} in catalog {}",
            request.entityIds().size(), hierarchyName, catalogId);

        return hierarchyService.removeEntityIds(catalogId, hierarchyName, request.entityIds())
            .map(count -> new EntityIdsOperationResponse(
                catalogId,
                hierarchyName,
                "remove",
                count,
                String.format("Removed %d entity ID(s) from hierarchy '%s'", count, hierarchyName)
            ));
    }

    // ========================================
    // Entity Directory Mutation Operations
    // ========================================

    /**
     * Adds entries to an EntityDirectory hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param request the request containing entries to add
     * @return Mono emitting the operation response
     */
    @PostMapping("/{hierarchyName}/entries")
    public Mono<DirectoryOperationResponse> addDirectoryEntries(
        @PathVariable UUID catalogId,
        @PathVariable String hierarchyName,
        @RequestBody AddDirectoryEntriesRequest request)
    {
        logger.info("Received request to add {} entries to directory {} in catalog {}",
            request.entries().size(), hierarchyName, catalogId);

        return hierarchyService.addDirectoryEntries(catalogId, hierarchyName, request.entries())
            .map(count -> new DirectoryOperationResponse(
                catalogId,
                hierarchyName,
                "add",
                count,
                String.format("Added %d entry/entries to directory '%s'", count, hierarchyName)
            ));
    }

    /**
     * Removes entries from an EntityDirectory hierarchy.
     * Must specify either names OR entityIds, but not both.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param request the request containing entries to remove (by name or ID)
     * @return Mono emitting the operation response
     */
    @DeleteMapping("/{hierarchyName}/entries")
    public Mono<DirectoryOperationResponse> removeDirectoryEntries(
        @PathVariable UUID catalogId,
        @PathVariable String hierarchyName,
        @RequestBody RemoveDirectoryEntriesRequest request)
    {
        logger.info("Received request to remove entries from directory {} in catalog {}",
            hierarchyName, catalogId);

        // Validate that exactly one of names or entityIds is provided
        boolean hasNames = request.names() != null && !request.names().isEmpty();
        boolean hasIds = request.entityIds() != null && !request.entityIds().isEmpty();

        if (hasNames == hasIds) {
            return Mono.error(new ValidationException(
                "Must specify either 'names' or 'entityIds', but not both or neither"
            ));
        }

        Mono<Integer> operation = hasNames
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

    // ========================================
    // Entity Tree Mutation Operations
    // ========================================

    /**
     * Adds child nodes to an EntityTree hierarchy under a specific parent.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param request the request containing parent path and nodes to add
     * @return Mono emitting the operation response
     */
    @PostMapping("/{hierarchyName}/nodes")
    public Mono<TreeOperationResponse> addTreeNodes(
        @PathVariable UUID catalogId,
        @PathVariable String hierarchyName,
        @RequestBody AddTreeNodesRequest request)
    {
        logger.info("Received request to add {} nodes to tree {} under path {} in catalog {}",
            request.nodes().size(), hierarchyName, request.parentPath(), catalogId);

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

    /**
     * Removes nodes from an EntityTree hierarchy by paths.
     * Removal cascades to remove all descendants.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param request the request containing paths to remove
     * @return Mono emitting the operation response
     */
    @DeleteMapping("/{hierarchyName}/nodes")
    public Mono<TreeOperationResponse> removeTreeNodes(
        @PathVariable UUID catalogId,
        @PathVariable String hierarchyName,
        @RequestBody RemoveTreeNodesRequest request)
    {
        logger.info("Received request to remove {} nodes from tree {} in catalog {}",
            request.paths().size(), hierarchyName, catalogId);

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
}
