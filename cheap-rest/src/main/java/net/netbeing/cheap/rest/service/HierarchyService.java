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

import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.rest.exception.ResourceNotFoundException;
import net.netbeing.cheap.util.CheapException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service layer for Hierarchy operations.
 * Provides business logic for retrieving hierarchy contents.
 */
@Service
public class HierarchyService
{
    private static final Logger logger = LoggerFactory.getLogger(HierarchyService.class);

    private final CheapDao dao;
    private final CheapFactory factory;

    public HierarchyService(CheapDao dao, CheapFactory factory)
    {
        this.dao = dao;
        this.factory = factory;
    }

    /**
     * Gets a hierarchy by name from a catalog.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @return the hierarchy
     * @throws ResourceNotFoundException if catalog or hierarchy is not found
     */
    @Transactional(readOnly = true)
    public Hierarchy getHierarchy(@NotNull UUID catalogId, @NotNull String hierarchyName)
    {
        logger.debug("Getting hierarchy {} from catalog {}", hierarchyName, catalogId);

        try {
            Catalog catalog = dao.loadCatalog(catalogId);
            if (catalog == null) {
                throw new ResourceNotFoundException("Catalog not found: " + catalogId);
            }

            Hierarchy hierarchy = catalog.hierarchy(hierarchyName);
            if (hierarchy == null) {
                throw new ResourceNotFoundException("Hierarchy not found: " + hierarchyName);
            }

            return hierarchy;
        } catch (SQLException e) {
            logger.error("Failed to load catalog");
            throw new CheapException("Failed to load catalog: " + e.getMessage(), e);
        }
    }

    /**
     * Gets paginated contents of an EntityList hierarchy.
     *
     * @param hierarchy the EntityList hierarchy
     * @param page the page number
     * @param size the page size
     * @return paginated list of entity IDs
     */
    public List<UUID> getEntityListContents(EntityListHierarchy hierarchy, int page, int size)
    {
        List<UUID> allIds = new ArrayList<>();
        for (Entity entity : hierarchy) {
            allIds.add(entity.globalId());
        }

        int start = page * size;
        int end = Math.min(start + size, allIds.size());

        if (start >= allIds.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(allIds.subList(start, end));
    }

    /**
     * Gets paginated contents of an EntitySet hierarchy.
     *
     * @param hierarchy the EntitySet hierarchy
     * @param page the page number
     * @param size the page size
     * @return paginated list of entity IDs
     */
    public List<UUID> getEntitySetContents(EntitySetHierarchy hierarchy, int page, int size)
    {
        List<UUID> allIds = new ArrayList<>();
        for (Entity entity : hierarchy) {
            allIds.add(entity.globalId());
        }

        int start = page * size;
        int end = Math.min(start + size, allIds.size());

        if (start >= allIds.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(allIds.subList(start, end));
    }

    /**
     * Gets paginated contents of an EntityDirectory hierarchy.
     *
     * @param hierarchy the EntityDirectory hierarchy
     * @param page the page number
     * @param size the page size
     * @return paginated map of key to entity ID
     */
    public Map<String, UUID> getEntityDirectoryContents(EntityDirectoryHierarchy hierarchy, int page, int size)
    {
        List<Map.Entry<String, Entity>> allEntries = new ArrayList<>();
        for (Map.Entry<String, Entity> entry : hierarchy.entrySet()) {
            allEntries.add(entry);
        }

        int start = page * size;
        int end = Math.min(start + size, allEntries.size());

        Map<String, UUID> result = new LinkedHashMap<>();
        if (start < allEntries.size()) {
            for (int i = start; i < end; i++) {
                Map.Entry<String, Entity> entry = allEntries.get(i);
                result.put(entry.getKey(), entry.getValue().globalId());
            }
        }

        return result;
    }

    /**
     * Gets the full tree structure (not paginated).
     *
     * @param hierarchy the EntityTree hierarchy
     * @return the root node
     */
    public EntityTreeHierarchy.Node getEntityTreeContents(EntityTreeHierarchy hierarchy)
    {
        return hierarchy.root();
    }

    /**
     * Gets paginated contents of an AspectMap hierarchy.
     *
     * @param hierarchy the AspectMap hierarchy
     * @param page the page number
     * @param size the page size
     * @return paginated map of entity ID to aspect
     */
    public Map<UUID, Aspect> getAspectMapContents(AspectMapHierarchy hierarchy, int page, int size)
    {
        List<Map.Entry<Entity, Aspect>> allEntries = new ArrayList<>();
        for (Map.Entry<Entity, Aspect> entry : hierarchy.entrySet()) {
            allEntries.add(entry);
        }

        int start = page * size;
        int end = Math.min(start + size, allEntries.size());

        Map<UUID, Aspect> result = new LinkedHashMap<>();
        if (start < allEntries.size()) {
            for (int i = start; i < end; i++) {
                Map.Entry<Entity, Aspect> entry = allEntries.get(i);
                result.put(entry.getKey().globalId(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Gets the total count of items in a hierarchy.
     *
     * @param hierarchy the hierarchy
     * @return the total count
     */
    public long countHierarchyItems(Hierarchy hierarchy)
    {
        if (hierarchy instanceof EntityListHierarchy list) {
            int count = 0;
            for (Entity ignored : list) {
                count++;
            }
            return count;
        } else if (hierarchy instanceof EntitySetHierarchy set) {
            int count = 0;
            for (Entity ignored : set) {
                count++;
            }
            return count;
        } else if (hierarchy instanceof EntityDirectoryHierarchy dir) {
            int count = 0;
            for (Map.Entry<String, Entity> ignored : dir.entrySet()) {
                count++;
            }
            return count;
        } else if (hierarchy instanceof AspectMapHierarchy map) {
            int count = 0;
            for (Map.Entry<Entity, Aspect> ignored : map.entrySet()) {
                count++;
            }
            return count;
        } else {
            // EntityTree - not counted
            return 0;
        }
    }
}
