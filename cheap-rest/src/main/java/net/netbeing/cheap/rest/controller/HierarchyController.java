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
import net.netbeing.cheap.rest.dto.*;
import net.netbeing.cheap.rest.service.HierarchyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Hierarchy operations.
 */
@RestController
@RequestMapping("/api/catalogs/{catalogId}/hierarchies")
public class HierarchyController
{
    private static final Logger logger = LoggerFactory.getLogger(HierarchyController.class);

    private final HierarchyService hierarchyService;

    @Value("${cheap.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${cheap.pagination.max-page-size:100}")
    private int maxPageSize;

    public HierarchyController(HierarchyService hierarchyService)
    {
        this.hierarchyService = hierarchyService;
    }

    /**
     * Gets hierarchy contents by name.
     * Returns different response types based on hierarchy type.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param page the page number (default: 0) - ignored for EntityTree
     * @param size the page size (default: from config) - ignored for EntityTree
     * @return hierarchy contents with appropriate structure
     */
    @GetMapping("/{hierarchyName}")
    public ResponseEntity<?> getHierarchy(
        @PathVariable UUID catalogId,
        @PathVariable String hierarchyName,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size)
    {
        logger.info("Received request to get hierarchy {} from catalog {}", hierarchyName, catalogId);

        Hierarchy hierarchy = hierarchyService.getHierarchy(catalogId, hierarchyName);

        // Handle based on hierarchy type
        if (hierarchy instanceof EntityListHierarchy entityList) {
            return handleEntityList(catalogId, hierarchyName, entityList, page, size);
        } else if (hierarchy instanceof EntitySetHierarchy entitySet) {
            return handleEntitySet(catalogId, hierarchyName, entitySet, page, size);
        } else if (hierarchy instanceof EntityDirectoryHierarchy entityDir) {
            return handleEntityDirectory(catalogId, hierarchyName, entityDir, page, size);
        } else if (hierarchy instanceof EntityTreeHierarchy entityTree) {
            return handleEntityTree(catalogId, hierarchyName, entityTree);
        } else if (hierarchy instanceof AspectMapHierarchy aspectMap) {
            return handleAspectMap(catalogId, hierarchyName, aspectMap, page, size);
        } else {
            throw new IllegalStateException("Unknown hierarchy type: " + hierarchy.getClass().getName());
        }
    }

    private ResponseEntity<EntityListResponse> handleEntityList(
        UUID catalogId, String hierarchyName, EntityListHierarchy hierarchy, int page, Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        List<UUID> content = hierarchyService.getEntityListContents(hierarchy, page, pageSize);
        long totalElements = hierarchyService.countHierarchyItems(hierarchy);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        EntityListResponse response = new EntityListResponse(
            catalogId,
            hierarchyName,
            content,
            page,
            pageSize,
            totalElements,
            totalPages
        );

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<EntityListResponse> handleEntitySet(
        UUID catalogId, String hierarchyName, EntitySetHierarchy hierarchy, int page, Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        List<UUID> content = hierarchyService.getEntitySetContents(hierarchy, page, pageSize);
        long totalElements = hierarchyService.countHierarchyItems(hierarchy);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        EntityListResponse response = new EntityListResponse(
            catalogId,
            hierarchyName,
            content,
            page,
            pageSize,
            totalElements,
            totalPages
        );

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<EntityDirectoryResponse> handleEntityDirectory(
        UUID catalogId, String hierarchyName, EntityDirectoryHierarchy hierarchy, int page, Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        Map<String, UUID> content = hierarchyService.getEntityDirectoryContents(hierarchy, page, pageSize);
        long totalElements = hierarchyService.countHierarchyItems(hierarchy);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        EntityDirectoryResponse response = new EntityDirectoryResponse(
            catalogId,
            hierarchyName,
            content,
            page,
            pageSize,
            totalElements,
            totalPages
        );

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<EntityTreeResponse> handleEntityTree(
        UUID catalogId, String hierarchyName, EntityTreeHierarchy hierarchy)
    {
        EntityTreeHierarchy.Node root = hierarchyService.getEntityTreeContents(hierarchy);

        EntityTreeResponse response = new EntityTreeResponse(
            catalogId,
            hierarchyName,
            root
        );

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<AspectMapResponse> handleAspectMap(
        UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy, int page, Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        Map<UUID, Aspect> content = hierarchyService.getAspectMapContents(hierarchy, page, pageSize);
        long totalElements = hierarchyService.countHierarchyItems(hierarchy);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        AspectMapResponse response = new AspectMapResponse(
            catalogId,
            hierarchyName,
            hierarchy.aspectDef().name(),
            content,
            page,
            pageSize,
            totalElements,
            totalPages
        );

        return ResponseEntity.ok(response);
    }
}
