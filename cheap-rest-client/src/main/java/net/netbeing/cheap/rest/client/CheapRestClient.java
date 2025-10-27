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
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * REST client for interacting with the Cheap REST API.
 * Provides methods for managing catalogs, aspect definitions, aspects, and hierarchies.
 */
public interface CheapRestClient
{
    // ========== Catalog Operations ==========

    /**
     * Creates a new catalog.
     *
     * @param catalogDef the catalog definition
     * @param species the catalog species
     * @param upstream optional upstream catalog ID
     * @param uri optional catalog URI
     * @return the create catalog response
     */
    @NotNull
    CreateCatalogResponse createCatalog(
        @NotNull CatalogDef catalogDef,
        @NotNull CatalogSpecies species,
        UUID upstream,
        java.net.URI uri
    );

    /**
     * Lists catalogs with pagination.
     *
     * @param page page number (0-based)
     * @param size page size
     * @return the catalog list response
     */
    @NotNull
    CatalogListResponse listCatalogs(int page, int size);

    /**
     * Gets a single catalog by ID.
     *
     * @param catalogId the catalog ID
     * @return the catalog definition
     */
    @NotNull
    CatalogDef getCatalog(@NotNull UUID catalogId);

    // ========== AspectDef Operations ==========

    /**
     * Creates a new aspect definition in a catalog.
     *
     * @param catalogId the catalog ID
     * @param aspectDef the aspect definition
     * @return the create aspect def response
     */
    @NotNull
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
    @NotNull
    AspectDefListResponse listAspectDefs(@NotNull UUID catalogId, int page, int size);

    /**
     * Gets a single aspect definition by ID.
     *
     * @param catalogId the catalog ID
     * @param aspectDefId the aspect def ID
     * @return the aspect definition
     */
    @NotNull
    AspectDef getAspectDef(@NotNull UUID catalogId, @NotNull UUID aspectDefId);

    /**
     * Gets a single aspect definition by name.
     *
     * @param catalogId the catalog ID
     * @param aspectDefName the aspect def name
     * @return the aspect definition
     */
    @NotNull
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
    @NotNull
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
    @NotNull
    AspectQueryResponse queryAspects(
        @NotNull UUID catalogId,
        @NotNull Set<UUID> entityIds,
        @NotNull Set<String> aspectDefNames
    );

    // ========== Hierarchy Operations ==========

    /**
     * Gets the contents of an EntityList or EntitySet hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param page page number (0-based)
     * @param size page size
     * @return the entity list response
     */
    @NotNull
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
    @NotNull
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
    @NotNull
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
    @NotNull
    AspectMapResponse getAspectMap(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        int page,
        int size
    );
}
