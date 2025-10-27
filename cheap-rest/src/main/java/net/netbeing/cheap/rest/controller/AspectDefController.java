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
import net.netbeing.cheap.json.dto.AspectDefListResponse;
import net.netbeing.cheap.json.dto.CreateAspectDefResponse;
import net.netbeing.cheap.rest.service.ReactiveAspectDefService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
 * REST controller for AspectDef operations.
 * Uses reactive types (Mono) to provide non-blocking HTTP handling.
 */
@RestController
@RequestMapping("/api/catalog/{catalogId}/aspect-defs")
public class AspectDefController
{
    private static final Logger logger = LoggerFactory.getLogger(AspectDefController.class);

    private final ReactiveAspectDefService aspectDefService;

    @Value("${cheap.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${cheap.pagination.max-page-size:100}")
    private int maxPageSize;

    public AspectDefController(ReactiveAspectDefService aspectDefService)
    {
        this.aspectDefService = aspectDefService;
    }

    /**
     * Creates a new AspectDef in a catalog reactively.
     *
     * @param catalogId the catalog ID
     * @param aspectDef the AspectDef to create
     * @return Mono emitting the created AspectDef metadata
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateAspectDefResponse> createAspectDef(
        @PathVariable UUID catalogId,
        @RequestBody AspectDef aspectDef)
    {
        logger.info("Received request to create AspectDef {} in catalog {}",
            aspectDef.name(), catalogId);

        return aspectDefService.createAspectDef(catalogId, aspectDef)
            .map(created -> new CreateAspectDefResponse(
                created.globalId(),
                created.name(),
                "AspectDef created successfully"
            ));
    }

    /**
     * Lists all AspectDefs in a catalog with pagination reactively.
     *
     * @param catalogId the catalog ID
     * @param page the page number (default: 0)
     * @param size the page size (default: from config)
     * @return Mono emitting paginated list of AspectDefs
     */
    @GetMapping
    public Mono<AspectDefListResponse> listAspectDefs(
        @PathVariable UUID catalogId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        return Mono.zip(
                aspectDefService.listAspectDefs(catalogId, page, pageSize),
                aspectDefService.countAspectDefs(catalogId)
            )
            .map(tuple -> {
                var aspectDefs = tuple.getT1();
                long totalElements = tuple.getT2();
                int totalPages = (int) Math.ceil((double) totalElements / pageSize);

                return new AspectDefListResponse(
                    catalogId,
                    aspectDefs,
                    page,
                    pageSize,
                    totalElements,
                    totalPages
                );
            });
    }

    /**
     * Gets an AspectDef by ID or name reactively.
     *
     * @param catalogId the catalog ID
     * @param aspectDefId the AspectDef UUID or name
     * @return Mono emitting the AspectDef
     */
    @GetMapping("/{aspectDefId}")
    public Mono<AspectDef> getAspectDef(
        @PathVariable UUID catalogId,
        @PathVariable String aspectDefId)
    {
        logger.info("Received request to get AspectDef {} in catalog {}", aspectDefId, catalogId);

        // Try parsing as UUID first
        try {
            UUID uuid = UUID.fromString(aspectDefId);
            return aspectDefService.getAspectDefById(catalogId, uuid);
        } catch (IllegalArgumentException _) {
            // Not a UUID, treat as name
            return aspectDefService.getAspectDefByName(catalogId, aspectDefId);
        }
    }
}
