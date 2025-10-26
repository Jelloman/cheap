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

import net.netbeing.cheap.model.AspectDef;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.UUID;

/**
 * Reactive wrapper for AspectDefService.
 * Wraps blocking JDBC operations in reactive types using a dedicated scheduler.
 *
 * This service delegates all operations to the underlying AspectDefService,
 * but wraps them in Mono to provide a reactive API. The blocking operations
 * are executed on a separate scheduler to avoid blocking the reactive event loop.
 */
@Service
public class ReactiveAspectDefService
{
    private final AspectDefService aspectDefService;
    private final Scheduler jdbcScheduler;

    public ReactiveAspectDefService(AspectDefService aspectDefService, Scheduler jdbcScheduler)
    {
        this.aspectDefService = aspectDefService;
        this.jdbcScheduler = jdbcScheduler;
    }

    /**
     * Creates a new AspectDef in a catalog reactively.
     *
     * @param catalogId the catalog ID
     * @param aspectDef the aspect definition to create
     * @return Mono emitting the created AspectDef
     */
    public Mono<AspectDef> createAspectDef(@NotNull UUID catalogId, @NotNull AspectDef aspectDef)
    {
        return Mono.fromCallable(() -> aspectDefService.createAspectDef(catalogId, aspectDef))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Lists all AspectDefs in a catalog with pagination reactively.
     *
     * @param catalogId the catalog ID
     * @param page the page number (zero-indexed)
     * @param size the page size
     * @return Mono emitting list of AspectDefs for the requested page
     */
    public Mono<List<AspectDef>> listAspectDefs(@NotNull UUID catalogId, int page, int size)
    {
        return Mono.fromCallable(() -> aspectDefService.listAspectDefs(catalogId, page, size))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Counts all AspectDefs in a catalog reactively.
     *
     * @param catalogId the catalog ID
     * @return Mono emitting the total number of AspectDefs
     */
    public Mono<Long> countAspectDefs(@NotNull UUID catalogId)
    {
        return Mono.fromCallable(() -> aspectDefService.countAspectDefs(catalogId))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets an AspectDef by name reactively.
     *
     * @param catalogId the catalog ID
     * @param name the AspectDef name
     * @return Mono emitting the AspectDef
     */
    public Mono<AspectDef> getAspectDefByName(@NotNull UUID catalogId, @NotNull String name)
    {
        return Mono.fromCallable(() -> aspectDefService.getAspectDefByName(catalogId, name))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets an AspectDef by ID reactively.
     *
     * @param catalogId the catalog ID
     * @param aspectDefId the AspectDef ID
     * @return Mono emitting the AspectDef
     */
    public Mono<AspectDef> getAspectDefById(@NotNull UUID catalogId, @NotNull UUID aspectDefId)
    {
        return Mono.fromCallable(() -> aspectDefService.getAspectDefById(catalogId, aspectDefId))
            .subscribeOn(jdbcScheduler);
    }
}
