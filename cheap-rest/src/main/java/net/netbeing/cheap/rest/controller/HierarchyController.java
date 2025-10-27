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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for Hierarchy operations.
 * Uses reactive types (Mono) to provide non-blocking HTTP handling.
 */
@RestController
@RequestMapping("/api/catalogs/{catalogId}/hierarchies")
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
}
