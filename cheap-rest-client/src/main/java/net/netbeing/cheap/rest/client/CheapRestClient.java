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

package net.netbeing.cheap.rest.client;

import net.netbeing.cheap.model.*;
import net.netbeing.cheap.json.dto.*;
import net.netbeing.cheap.rest.client.exception.CheapRestBadRequestException;
import net.netbeing.cheap.rest.client.exception.CheapRestNotFoundException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * REST client for interacting with the Cheap REST API.
 * Provides methods for managing catalogs, aspect definitions, aspects, and hierarchies.
 */
@SuppressWarnings("unused")
public interface CheapRestClient
{
    // ========== Configuration Operations ==========
    /**
     * Register an AspectDef with this client, allowing Aspects of that type to be deserialized.
     */
    void registerAspectDef(@NotNull AspectDef aspectDef);

    // ========== Catalog Operations ==========

    /**
     * Creates a new catalog.
     *
     * @param catalogDef the catalog definition
     * @param species the catalog species
     * @param upstream optional upstream catalog ID
     * @return the created catalog response
     */
    CreateCatalogResponse createCatalog(
        @NotNull CatalogDef catalogDef,
        @NotNull CatalogSpecies species,
        UUID upstream
    );

    /**
     * Lists catalogs with pagination.
     *
     * @param page page number (0-based)
     * @param size page size
     * @return the catalog list response
     */
    CatalogListResponse listCatalogs(int page, int size);

    /**
     * Gets a single catalog by ID.
     *
     * @param catalogId the catalog ID
     * @return the catalog definition
     */
    CatalogDef getCatalogDef(@NotNull UUID catalogId);

    // ========== AspectDef Operations ==========

    /**
     * Creates a new aspect definition in a catalog.
     *
     * @param catalogId the catalog ID
     * @param aspectDef the aspect definition
     * @return the created aspect def response
     */
    CreateAspectDefResponse createAspectDef(
        @NotNull UUID catalogId,
        @NotNull AspectDef aspectDef
    );

    /**
     * Lists aspect definitions in a catalog with pagination.
     *
     * @param catalogId the catalog ID
     * @param page page number (0-based)
     * @param size page size
     * @return the aspect def list response
     */
    AspectDefListResponse listAspectDefs(@NotNull UUID catalogId, int page, int size);

    /**
     * Gets a single aspect definition by ID.
     *
     * @param catalogId the catalog ID
     * @param aspectDefId the aspect def ID
     * @return the aspect definition
     */
    AspectDef getAspectDef(@NotNull UUID catalogId, @NotNull UUID aspectDefId);

    /**
     * Gets a single aspect definition by name.
     *
     * @param catalogId the catalog ID
     * @param aspectDefName the aspect def name
     * @return the aspect definition
     */
    AspectDef getAspectDefByName(@NotNull UUID catalogId, @NotNull String aspectDefName);

    // ========== Aspect Operations ==========

    /**
     * Upserts (creates or updates) aspects for entities.
     *
     * @param catalogId the catalog ID
     * @param aspectDefName the aspect definition name
     * @param aspects map of entity ID to property values
     * @return the upsert aspects response
     */
    UpsertAspectsResponse upsertAspects(
        @NotNull UUID catalogId,
        @NotNull String aspectDefName,
        @NotNull Map<UUID, Map<String, Object>> aspects
    );

    /**
     * Queries aspects for multiple entities and aspect definitions.
     *
     * @param catalogId the catalog ID
     * @param entityIds set of entity IDs to query
     * @param aspectDefNames set of aspect definition names to query
     * @return the aspect query response
     */
    AspectQueryResponse queryAspects(
        @NotNull UUID catalogId,
        @NotNull Set<UUID> entityIds,
        @NotNull Set<String> aspectDefNames
    );

    // ========== Hierarchy Operations ==========

    /**
     * Creates a new hierarchy in a catalog.
     *
     * @param catalogId the catalog ID
     * @param hierarchyDef the hierarchy definition
     * @return the created hierarchy response
     * @throws CheapRestNotFoundException if catalog not found
     * @throws CheapRestBadRequestException if invalid hierarchy definition
     */
    CreateHierarchyResponse createHierarchy(
        @NotNull UUID catalogId,
        @NotNull HierarchyDef hierarchyDef
    );

    /**
     * Gets the contents of an EntityList or EntitySet hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param page page number (0-based)
     * @param size page size
     * @return the entity list response
     */
    EntityListResponse getEntityList(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        int page,
        int size
    );

    /**
     * Gets the contents of an EntityDirectory hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @return the entity directory response
     */
    EntityDirectoryResponse getEntityDirectory(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName
    );

    /**
     * Gets the contents of an EntityTree hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @return the entity tree response
     */
    EntityTreeResponse getEntityTree(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName
    );

    /**
     * Gets the contents of an AspectMap hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param page page number (0-based)
     * @param size page size
     * @return the aspect map response
     */
    AspectMapResponse getAspectMap(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        int page,
        int size
    );

    // ========== Hierarchy Mutation Operations ==========

    /**
     * Adds entity IDs to an EntityList or EntitySet hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityIds the list of entity IDs to add
     * @return the operation response with success status and count
     * @throws CheapRestNotFoundException if catalog or hierarchy not found
     * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
     */
    EntityIdsOperationResponse addEntityIds(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<UUID> entityIds
    );

    /**
     * Removes entity IDs from an EntityList or EntitySet hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityIds the list of entity IDs to remove
     * @return the operation response with success status and count
     * @throws CheapRestNotFoundException if catalog or hierarchy not found
     * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
     */
    EntityIdsOperationResponse removeEntityIds(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<UUID> entityIds
    );

    /**
     * Adds entries to an EntityDirectory hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entries map of entry names to entity IDs
     * @return the operation response with success status and count
     * @throws CheapRestNotFoundException if catalog or hierarchy not found
     * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
     */
    DirectoryOperationResponse addDirectoryEntries(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull Map<String, UUID> entries
    );

    /**
     * Removes directory entries by their names.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param names the list of entry names to remove
     * @return the operation response with success status and count
     * @throws CheapRestNotFoundException if catalog or hierarchy not found
     * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
     */
    DirectoryOperationResponse removeDirectoryEntriesByNames(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<String> names
    );

    /**
     * Removes directory entries by their entity IDs (removes all entries pointing to these IDs).
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityIds the list of entity IDs to remove
     * @return the operation response with success status and count
     * @throws CheapRestNotFoundException if catalog or hierarchy not found
     * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
     */
    DirectoryOperationResponse removeDirectoryEntriesByEntityIds(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<UUID> entityIds
    );

    /**
     * Adds nodes to an EntityTree hierarchy under a specified parent path.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param parentPath the parent node path (null or empty for root-level nodes, e.g., "/root/folder1")
     * @param nodes map of child names to entity IDs
     * @return the operation response with success status and count
     * @throws CheapRestNotFoundException if catalog, hierarchy, or parent node not found
     * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
     */
    TreeOperationResponse addTreeNodes(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        String parentPath,
        @NotNull Map<String, UUID> nodes
    );

    /**
     * Removes nodes from an EntityTree hierarchy by their paths (cascade deletes all descendants).
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param paths the list of node paths to remove (e.g., ["/root/folder1", "/root/folder2"])
     * @return the operation response with success status and count
     * @throws CheapRestNotFoundException if catalog or hierarchy not found
     * @throws CheapRestBadRequestException if wrong hierarchy type or invalid request
     */
    TreeOperationResponse removeTreeNodes(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<String> paths
    );
}
