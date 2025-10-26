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

import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.rest.dto.CatalogListResponse;
import net.netbeing.cheap.rest.dto.CreateCatalogRequest;
import net.netbeing.cheap.rest.dto.CreateCatalogResponse;
import net.netbeing.cheap.rest.dto.GetCatalogDefResponse;
import net.netbeing.cheap.rest.service.ReactiveCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * REST controller for Catalog operations.
 * Uses reactive types (Mono) to provide non-blocking HTTP handling.
 */
@RestController
@RequestMapping("/api/catalogs")
public class CatalogController
{
    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);

    private final ReactiveCatalogService catalogService;

    @Value("${cheap.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${cheap.pagination.max-page-size:100}")
    private int maxPageSize;

    public CatalogController(ReactiveCatalogService catalogService)
    {
        this.catalogService = catalogService;
    }

    /**
     * Creates a new catalog reactively.
     *
     * @param request the catalog creation request
     * @return Mono emitting the created catalog ID and metadata
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateCatalogResponse> createCatalog(@RequestBody CreateCatalogRequest request)
    {
        logger.info("Received request to create catalog with species: {}", request.species());

        return catalogService.createCatalog(
                request.catalogDef(),
                request.species(),
                request.upstream(),
                request.uri()
            )
            .map(catalogId -> new CreateCatalogResponse(
                catalogId,
                request.uri(),
                "Catalog created successfully"
            ));
    }

    /**
     * Lists all catalog IDs with pagination reactively.
     *
     * @param page the page number (default: 0)
     * @param size the page size (default: from config)
     * @return Mono emitting paginated list of catalog IDs
     */
    @GetMapping
    public Mono<CatalogListResponse> listCatalogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size)
    {
        int pageSize = size != null ? size : defaultPageSize;

        // Validate page size
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("Page size " + pageSize + " exceeds maximum of " + maxPageSize);
        }

        return Mono.zip(
                catalogService.listCatalogIds(page, pageSize),
                catalogService.countCatalogs()
            )
            .map(tuple -> {
                var catalogIds = tuple.getT1();
                long totalElements = tuple.getT2();
                int totalPages = (int) Math.ceil((double) totalElements / pageSize);

                return new CatalogListResponse(
                    catalogIds,
                    page,
                    pageSize,
                    totalElements,
                    totalPages
                );
            });
    }

    /**
     * Gets a catalog definition by ID reactively.
     *
     * @param catalogId the catalog ID
     * @return Mono emitting the catalog definition response DTO
     */
    @GetMapping("/{catalogId}")
    public Mono<GetCatalogDefResponse> getCatalog(@PathVariable UUID catalogId)
    {
        logger.info("Received request to get catalog {}", catalogId);

        return catalogService.getCatalogDefResponse(catalogId);
    }
}
