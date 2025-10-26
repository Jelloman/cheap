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

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.EntityDirectoryHierarchy;
import net.netbeing.cheap.model.EntityListHierarchy;
import net.netbeing.cheap.model.EntitySetHierarchy;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.Hierarchy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reactive wrapper for HierarchyService.
 * Wraps blocking JDBC operations in reactive types using a dedicated scheduler.
 *
 * This service delegates all operations to the underlying HierarchyService,
 * but wraps them in Mono to provide a reactive API. The blocking operations
 * are executed on a separate scheduler to avoid blocking the reactive event loop.
 */
@Service
public class ReactiveHierarchyService
{
    private final HierarchyService hierarchyService;
    private final Scheduler jdbcScheduler;

    public ReactiveHierarchyService(HierarchyService hierarchyService, Scheduler jdbcScheduler)
    {
        this.hierarchyService = hierarchyService;
        this.jdbcScheduler = jdbcScheduler;
    }

    /**
     * Gets a hierarchy by name from a catalog reactively.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @return Mono emitting the hierarchy
     */
    public Mono<Hierarchy> getHierarchy(@NotNull UUID catalogId, @NotNull String hierarchyName)
    {
        return Mono.fromCallable(() -> hierarchyService.getHierarchy(catalogId, hierarchyName))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets paginated contents of an EntityList hierarchy reactively.
     *
     * @param hierarchy the EntityList hierarchy
     * @param page the page number
     * @param size the page size
     * @return Mono emitting paginated list of entity IDs
     */
    public Mono<List<UUID>> getEntityListContents(EntityListHierarchy hierarchy, int page, int size)
    {
        return Mono.fromCallable(() -> hierarchyService.getEntityListContents(hierarchy, page, size))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets paginated contents of an EntitySet hierarchy reactively.
     *
     * @param hierarchy the EntitySet hierarchy
     * @param page the page number
     * @param size the page size
     * @return Mono emitting paginated list of entity IDs
     */
    public Mono<List<UUID>> getEntitySetContents(EntitySetHierarchy hierarchy, int page, int size)
    {
        return Mono.fromCallable(() -> hierarchyService.getEntitySetContents(hierarchy, page, size))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets paginated contents of an EntityDirectory hierarchy reactively.
     *
     * @param hierarchy the EntityDirectory hierarchy
     * @param page the page number
     * @param size the page size
     * @return Mono emitting paginated map of key to entity ID
     */
    public Mono<Map<String, UUID>> getEntityDirectoryContents(EntityDirectoryHierarchy hierarchy, int page, int size)
    {
        return Mono.fromCallable(() -> hierarchyService.getEntityDirectoryContents(hierarchy, page, size))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets the full tree structure (not paginated) reactively.
     *
     * @param hierarchy the EntityTree hierarchy
     * @return Mono emitting the root node
     */
    public Mono<EntityTreeHierarchy.Node> getEntityTreeContents(EntityTreeHierarchy hierarchy)
    {
        return Mono.fromCallable(() -> hierarchyService.getEntityTreeContents(hierarchy))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets paginated contents of an AspectMap hierarchy reactively.
     *
     * @param hierarchy the AspectMap hierarchy
     * @param page the page number
     * @param size the page size
     * @return Mono emitting paginated map of entity ID to aspect
     */
    public Mono<Map<UUID, Aspect>> getAspectMapContents(AspectMapHierarchy hierarchy, int page, int size)
    {
        return Mono.fromCallable(() -> hierarchyService.getAspectMapContents(hierarchy, page, size))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Gets the total count of items in a hierarchy reactively.
     *
     * @param hierarchy the hierarchy
     * @return Mono emitting the total count
     */
    public Mono<Long> countHierarchyItems(Hierarchy hierarchy)
    {
        return Mono.fromCallable(() -> hierarchyService.countHierarchyItems(hierarchy))
            .subscribeOn(jdbcScheduler);
    }
}
