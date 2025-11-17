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

package net.netbeing.cheap.rest.service;

import net.netbeing.cheap.json.dto.GetCatalogDefResponse;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.HierarchyDef;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reactive wrapper for CatalogService.
 * Wraps blocking JDBC operations in reactive types using a dedicated scheduler.
 *
 * This service delegates all operations to the underlying CatalogService,
 * but wraps them in Mono to provide a reactive API. The blocking operations
 * are executed on a separate scheduler to avoid blocking the reactive event loop.
 */
@Service
public class ReactiveCatalogService
{
    private final CatalogService catalogService;
    private final Scheduler jdbcScheduler;

    public ReactiveCatalogService(CatalogService catalogService, Scheduler jdbcScheduler)
    {
        this.catalogService = catalogService;
        this.jdbcScheduler = jdbcScheduler;
    }

    /**
     * Creates a new catalog reactively.
     *
     * @param catalogDef the catalog definition
     * @param species the catalog species
     * @param upstream the upstream catalog ID (may be null for SOURCE/SINK)
     * @param baseCatalogURL optional URI for the catalog
     * @return Mono emitting the UUID of the newly created catalog
     */
    public Mono<UUID> createCatalog(@NotNull CatalogDef catalogDef, @NotNull CatalogSpecies species,
                                    UUID upstream, URI baseCatalogURL)
    {
        return Mono.fromCallable(() -> catalogService.createCatalog(catalogDef, species, upstream, baseCatalogURL))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Lists catalog IDs reactively with pagination.
     *
     * @param page the page number (zero-indexed)
     * @param size the page size
     * @return Mono emitting list of catalog IDs for the requested page
     */
    public Mono<List<UUID>> listCatalogIds(int page, int size)
    {
        return Mono.fromCallable(() -> catalogService.listCatalogIds(page, size))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Counts all catalogs reactively.
     *
     * @return Mono emitting the total number of catalogs
     */
    public Mono<Long> countCatalogs()
    {
        return Mono.fromCallable(catalogService::countCatalogs)
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets a catalog definition by ID reactively.
     *
     * @param catalogId the catalog ID
     * @return Mono emitting the catalog definition
     */
    public Mono<CatalogDef> getCatalogDef(@NotNull UUID catalogId)
    {
        return Mono.fromCallable(() -> catalogService.getCatalogDef(catalogId))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets a catalog definition by ID as a response DTO reactively.
     * This method converts the CatalogDef to a DTO with Lists instead of Iterables
     * to avoid WebFlux treating it as a streaming response.
     *
     * @param catalogId the catalog ID
     * @return Mono emitting the catalog definition response DTO
     */
    public Mono<GetCatalogDefResponse> getCatalogDefResponse(@NotNull UUID catalogId)
    {
        return getCatalogDef(catalogId)
            .map(catalogDef -> {
                var hierarchyDefs = new ArrayList<HierarchyDef>();
                catalogDef.hierarchyDefs().forEach(hierarchyDefs::add);

                var aspectDefs = new java.util.ArrayList<AspectDef>();
                catalogDef.aspectDefs().forEach(aspectDefs::add);

                return new GetCatalogDefResponse(hierarchyDefs, aspectDefs);
            });
    }
}
