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

import net.netbeing.cheap.json.dto.UpsertAspectsResponse.AspectResult;
import net.netbeing.cheap.model.AspectMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reactive wrapper for AspectService.
 * Wraps blocking JDBC operations in reactive types using a dedicated scheduler.
 *
 * This service delegates all operations to the underlying AspectService,
 * but wraps them in Mono to provide a reactive API. The blocking operations
 * are executed on a separate scheduler to avoid blocking the reactive event loop.
 */
@Service
public class ReactiveAspectService
{
    private final AspectService aspectService;
    private final Scheduler jdbcScheduler;

    public ReactiveAspectService(AspectService aspectService, Scheduler jdbcScheduler)
    {
        this.aspectService = aspectService;
        this.jdbcScheduler = jdbcScheduler;
    }

    /**
     * Upserts aspects for multiple entities reactively.
     *
     * @param catalogId       the catalog ID
     * @param aspectDefName   the AspectDef name
     * @param aspectsByEntity map of entity ID to property values
     * @return Mono emitting map of entity ID to upsert result
     */
    public Mono<Map<UUID, AspectResult>> upsertAspects(
        @NotNull UUID catalogId,
        @NotNull String aspectDefName,
        @NotNull Map<UUID, Map<String, Object>> aspectsByEntity)
    {
        return Mono.fromCallable(() -> aspectService.upsertAspects(catalogId, aspectDefName, aspectsByEntity))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Queries aspects for multiple entities reactively.
     *
     * @param catalogId      the catalog ID
     * @param entityIds      the set of entity IDs to query
     * @param aspectDefNames the set of AspectDef names to retrieve (empty = all)
     * @return Mono emitting map of entity ID to map of AspectDef name to Aspect
     */
    public Mono<List<AspectMap>> queryAspects(
        @NotNull UUID catalogId,
        @NotNull Set<UUID> entityIds,
        Set<String> aspectDefNames)
    {
        return Mono.fromCallable(() -> aspectService.queryAspects(catalogId, entityIds, aspectDefNames))
            .subscribeOn(jdbcScheduler);
    }
}
