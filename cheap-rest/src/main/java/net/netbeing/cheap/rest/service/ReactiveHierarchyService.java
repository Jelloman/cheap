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
import net.netbeing.cheap.model.HierarchyDef;
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

    /**
     * Creates a new hierarchy from a HierarchyDef and adds it to the catalog reactively.
     *
     * @param catalogId the catalog ID
     * @param hierarchyDef the hierarchy definition
     * @return Mono emitting the name of the created hierarchy
     */
    public Mono<String> createHierarchy(@NotNull UUID catalogId, @NotNull HierarchyDef hierarchyDef)
    {
        return Mono.fromCallable(() -> hierarchyService.createHierarchy(catalogId, hierarchyDef))
            .subscribeOn(jdbcScheduler);
    }

    // ========================================
    // Entity List/Set Mutation Operations
    // ========================================

    /**
     * Adds entity IDs to an EntityList or EntitySet hierarchy reactively.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityIds the list of entity IDs to add
     * @return Mono emitting the number of entity IDs added
     */
    public Mono<Integer> addEntityIds(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<UUID> entityIds)
    {
        return Mono.fromCallable(() -> hierarchyService.addEntityIds(catalogId, hierarchyName, entityIds))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Removes entity IDs from an EntityList or EntitySet hierarchy reactively.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityIds the list of entity IDs to remove
     * @return Mono emitting the number of entity IDs removed
     */
    public Mono<Integer> removeEntityIds(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<UUID> entityIds)
    {
        return Mono.fromCallable(() -> hierarchyService.removeEntityIds(catalogId, hierarchyName, entityIds))
            .subscribeOn(jdbcScheduler);
    }

    // ========================================
    // Entity Directory Mutation Operations
    // ========================================

    /**
     * Adds entries to an EntityDirectory hierarchy reactively.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entries the map of name -> entity ID pairs to add
     * @return Mono emitting the number of entries added
     */
    public Mono<Integer> addDirectoryEntries(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull Map<String, UUID> entries)
    {
        return Mono.fromCallable(() -> hierarchyService.addDirectoryEntries(catalogId, hierarchyName, entries))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Removes entries from an EntityDirectory hierarchy by names reactively.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param names the list of names to remove
     * @return Mono emitting the number of entries removed
     */
    public Mono<Integer> removeDirectoryEntriesByNames(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<String> names)
    {
        return Mono.fromCallable(() -> hierarchyService.removeDirectoryEntriesByNames(catalogId, hierarchyName, names))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Removes entries from an EntityDirectory hierarchy by entity IDs reactively.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityIds the list of entity IDs to remove
     * @return Mono emitting the number of entries removed
     */
    public Mono<Integer> removeDirectoryEntriesByIds(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<UUID> entityIds)
    {
        return Mono.fromCallable(() -> hierarchyService.removeDirectoryEntriesByIds(catalogId, hierarchyName, entityIds))
            .subscribeOn(jdbcScheduler);
    }

    // ========================================
    // Entity Tree Mutation Operations
    // ========================================

    /**
     * Adds child nodes to an EntityTree hierarchy reactively.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param parentPath the path to the parent node
     * @param nodes the map of child name -> entity ID pairs to add
     * @return Mono emitting the number of nodes added
     */
    public Mono<Integer> addTreeNodes(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull String parentPath, @NotNull Map<String, UUID> nodes)
    {
        return Mono.fromCallable(() -> hierarchyService.addTreeNodes(catalogId, hierarchyName, parentPath, nodes))
            .subscribeOn(jdbcScheduler);
    }

    /**
     * Removes nodes from an EntityTree hierarchy reactively.
     * Removal cascades to remove all descendants.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param paths the list of node paths to remove
     * @return Mono emitting the number of nodes removed (including descendants)
     */
    public Mono<Integer> removeTreeNodes(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<String> paths)
    {
        return Mono.fromCallable(() -> hierarchyService.removeTreeNodes(catalogId, hierarchyName, paths))
            .subscribeOn(jdbcScheduler);
    }
}
