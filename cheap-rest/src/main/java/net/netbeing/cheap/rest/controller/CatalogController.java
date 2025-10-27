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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.netbeing.cheap.json.dto.CatalogListResponse;
import net.netbeing.cheap.json.dto.CreateCatalogRequest;
import net.netbeing.cheap.json.dto.CreateCatalogResponse;
import net.netbeing.cheap.json.dto.GetCatalogDefResponse;
import net.netbeing.cheap.rest.service.ReactiveCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * REST controller for Catalog operations.
 * Uses reactive types (Mono) to provide non-blocking HTTP handling.
 */
@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Catalogs", description = "Catalog management endpoints for creating and querying catalogs")
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
    @Operation(summary = "Create a new catalog",
               description = "Creates a new catalog with the specified definition, species, and optional upstream catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Catalog created successfully",
                     content = @Content(schema = @Schema(implementation = CreateCatalogResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid catalog definition or validation failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<CreateCatalogResponse> createCatalog(
            @Parameter(description = "Catalog creation request with catalogDef, species, and optional URI/upstream")
            @RequestBody CreateCatalogRequest request, ServerHttpRequest httpRequest) throws URISyntaxException
    {
        logger.info("Received request to create catalog with species: {}", request.species());

        URI fullUri = httpRequest.getURI();
        URI endpointURL = new URI(fullUri.getScheme(), fullUri.getAuthority(), fullUri.getPath(), null, null);
        logger.debug("Endpoint URL {}", endpointURL);

        return catalogService.createCatalog(
                request.catalogDef(),
                request.species(),
                request.upstream(),
                endpointURL
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
    @Operation(summary = "List all catalogs",
               description = "Returns a paginated list of all catalog IDs with metadata (total count, page info)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Catalog list retrieved successfully",
                     content = @Content(schema = @Schema(implementation = CatalogListResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid page size (exceeds maximum)"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<CatalogListResponse> listCatalogs(
        @Parameter(description = "Page number (zero-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size (defaults to configured value)", example = "20")
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
    @Operation(summary = "Get catalog definition by ID",
               description = "Retrieves the complete catalog definition including all hierarchy and aspect definitions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Catalog definition retrieved successfully",
                     content = @Content(schema = @Schema(implementation = GetCatalogDefResponse.class))),
        @ApiResponse(responseCode = "404", description = "Catalog not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<GetCatalogDefResponse> getCatalog(
            @Parameter(description = "Catalog UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID catalogId)
    {
        logger.info("Received request to get catalog {}", catalogId);

        return catalogService.getCatalogDefResponse(catalogId);
    }
}
