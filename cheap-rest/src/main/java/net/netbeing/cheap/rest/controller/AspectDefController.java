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

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.rest.dto.AspectDefListResponse;
import net.netbeing.cheap.rest.dto.CreateAspectDefResponse;
import net.netbeing.cheap.rest.service.AspectDefService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for AspectDef operations.
 */
@RestController
@RequestMapping("/api/catalogs/{catalogId}/aspect-defs")
public class AspectDefController
{
    private static final Logger logger = LoggerFactory.getLogger(AspectDefController.class);

    private final AspectDefService aspectDefService;

    @Value("${cheap.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${cheap.pagination.max-page-size:100}")
    private int maxPageSize;

    public AspectDefController(AspectDefService aspectDefService)
    {
        this.aspectDefService = aspectDefService;
    }

    /**
     * Creates a new AspectDef in a catalog.
     *
     * @param catalogId the catalog ID
     * @param aspectDef the AspectDef to create
     * @return the created AspectDef metadata
     */
    @PostMapping
    public ResponseEntity<CreateAspectDefResponse> createAspectDef(
        @PathVariable UUID catalogId,
        @RequestBody AspectDef aspectDef)
    {
        logger.info("Received request to create AspectDef {} in catalog {}", 
            aspectDef.name(), catalogId);

        AspectDef created = aspectDefService.createAspectDef(catalogId, aspectDef);

        CreateAspectDefResponse response = new CreateAspectDefResponse(
            created.globalId(),
            created.name(),
            "AspectDef created successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all AspectDefs in a catalog with pagination.
     *
     * @param catalogId the catalog ID
     * @param page the page number (default: 0)
     * @param size the page size (default: from config)
     * @return paginated list of AspectDefs
     */
    @GetMapping
    public ResponseEntity<AspectDefListResponse> listAspectDefs(
        @PathVariable UUID catalogId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        List<AspectDef> aspectDefs = aspectDefService.listAspectDefs(catalogId, page, pageSize);
        long totalElements = aspectDefService.countAspectDefs(catalogId);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        AspectDefListResponse response = new AspectDefListResponse(
            catalogId,
            aspectDefs,
            page,
            pageSize,
            totalElements,
            totalPages
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets an AspectDef by ID or name.
     *
     * @param catalogId the catalog ID
     * @param aspectDefId the AspectDef UUID or name
     * @return the AspectDef
     */
    @GetMapping("/{aspectDefId}")
    public ResponseEntity<AspectDef> getAspectDef(
        @PathVariable UUID catalogId,
        @PathVariable String aspectDefId)
    {
        logger.info("Received request to get AspectDef {} in catalog {}", aspectDefId, catalogId);

        AspectDef aspectDef;

        // Try parsing as UUID first
        try {
            UUID uuid = UUID.fromString(aspectDefId);
            aspectDef = aspectDefService.getAspectDefById(catalogId, uuid);
        } catch (IllegalArgumentException _) {
            // Not a UUID, treat as name
            aspectDef = aspectDefService.getAspectDefByName(catalogId, aspectDefId);
        }

        return ResponseEntity.ok(aspectDef);
    }
}
