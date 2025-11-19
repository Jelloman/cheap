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

import jakarta.annotation.Resource;
import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityDirectoryHierarchy;
import net.netbeing.cheap.model.EntityListHierarchy;
import net.netbeing.cheap.model.EntitySetHierarchy;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.rest.exception.ResourceNotFoundException;
import net.netbeing.cheap.rest.exception.ValidationException;
import net.netbeing.cheap.util.CheapException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final CatalogService catalogService;
    private final CheapFactory factory;

    @Resource
    @Lazy
    private HierarchyService service;

    public HierarchyService(CheapDao dao, CatalogService catalogService, CheapFactory factory)
    {
        this.dao = dao;
        this.catalogService = catalogService;
        this.factory = factory;
    }

    /**
     * Sets the service reference for Spring proxy injection.
     * Package-private for testing purposes.
     *
     * @param hierarchyService the service reference
     */
    void setService(HierarchyService hierarchyService)
    {
        this.service = hierarchyService;
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

        Catalog catalog = catalogService.getCatalog(catalogId);

        Hierarchy hierarchy = catalog.hierarchy(hierarchyName);
        if (hierarchy == null) {
            throw new ResourceNotFoundException("Hierarchy not found: " + hierarchyName);
        }

        return hierarchy;
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
        return hierarchy.stream()
            .skip((long) page * size)
            .limit(size)
            .map(Entity::globalId)
            .toList();
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
        return hierarchy.stream()
            .skip((long) page * size)
            .limit(size)
            .map(Entity::globalId)
            .toList();
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
        final Map<String, UUID> contents = HashMap.newHashMap(size);

        hierarchy.entrySet().stream()
            .skip((long) page * size)
            .limit(size)
            .forEach(entry -> contents.put(entry.getKey(), entry.getValue().globalId()));

        return contents;
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
        List<Map.Entry<Entity, Aspect>> allEntries = new ArrayList<>(hierarchy.entrySet());

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
        return switch (hierarchy) {
            case EntityListHierarchy list -> list.size();
            case EntitySetHierarchy set -> set.size();
            case EntityDirectoryHierarchy dir -> dir.size();
            case AspectMapHierarchy map -> map.size();
            default -> 0;
        };
    }

    /**
     * Creates a new hierarchy from a HierarchyDef and adds it to the catalog.
     *
     * @param catalogId the catalog ID
     * @param hierarchyDef the hierarchy definition
     * @return the name of the created hierarchy
     * @throws ResourceNotFoundException if catalog is not found
     */
    @Transactional
    public String createHierarchy(@NotNull UUID catalogId, @NotNull HierarchyDef hierarchyDef)
    {
        logger.debug("Creating hierarchy {} in catalog {}", hierarchyDef.name(), catalogId);

        Catalog catalog = catalogService.getCatalog(catalogId);
        try {
            // Delegate to CatalogService to create and add the hierarchy
            catalogService.createAndAddHierarchy(catalog, hierarchyDef);

            // Save the updated catalog
            dao.saveCatalog(catalog);

            logger.info("Successfully created hierarchy {} in catalog {}", hierarchyDef.name(), catalogId);
            return hierarchyDef.name();
        } catch (SQLException e) {
            logger.error("Failed to create hierarchy");
            throw new CheapException("Failed to create hierarchy: " + e.getMessage(), e);
        }
    }

    // ========================================
    // Entity List/Set Mutation Operations
    // ========================================

    /**
     * Adds entity IDs to an EntityList or EntitySet hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityIds the list of entity IDs to add
     * @return the number of entity IDs added
     * @throws ResourceNotFoundException if catalog or hierarchy is not found
     * @throws ValidationException if hierarchy is not an EntityList or EntitySet
     */
    @Transactional
    public int addEntityIds(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<UUID> entityIds)
    {
        logger.debug("Adding {} entity IDs to hierarchy {} in catalog {}", entityIds.size(), hierarchyName, catalogId);

        Catalog catalog = catalogService.getCatalog(catalogId);
        Hierarchy hierarchy = service.getHierarchy(catalogId, hierarchyName);

        try {
            int count = switch (hierarchy) {
                case EntityListHierarchy list -> {
                    for (UUID entityId : entityIds) {
                        Entity entity = factory.getOrRegisterNewEntity(entityId);
                        list.add(entity);
                    }
                    yield entityIds.size();
                }
                case EntitySetHierarchy set -> {
                    for (UUID entityId : entityIds) {
                        Entity entity = factory.getOrRegisterNewEntity(entityId);
                        set.add(entity);
                    }
                    yield entityIds.size();
                }
                default -> throw new ValidationException(
                    "Hierarchy '" + hierarchyName + "' is not an EntityList or EntitySet"
                );
            };

            dao.saveCatalog(catalog);
            logger.info("Successfully added {} entity IDs to hierarchy {}", count, hierarchyName);
            return count;
        } catch (SQLException e) {
            logger.error("Failed to add entity IDs to hierarchy");
            throw new CheapException("Failed to add entity IDs: " + e.getMessage(), e);
        }
    }

    /**
     * Removes entity IDs from an EntityList or EntitySet hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityIds the list of entity IDs to remove
     * @return the number of entity IDs removed
     * @throws ResourceNotFoundException if catalog or hierarchy is not found
     * @throws ValidationException if hierarchy is not an EntityList or EntitySet
     */
    @Transactional
    public int removeEntityIds(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<UUID> entityIds)
    {
        logger.debug("Removing {} entity IDs from hierarchy {} in catalog {}", entityIds.size(), hierarchyName, catalogId);

        Catalog catalog = catalogService.getCatalog(catalogId);
        Hierarchy hierarchy = service.getHierarchy(catalogId, hierarchyName);

        try {
            int count = switch (hierarchy) {
                case EntityListHierarchy list -> {
                    int removed = 0;
                    for (UUID entityId : entityIds) {
                        Entity entity = factory.getOrRegisterNewEntity(entityId);
                        if (list.remove(entity)) {
                            removed++;
                        }
                    }
                    yield removed;
                }
                case EntitySetHierarchy set -> {
                    int removed = 0;
                    for (UUID entityId : entityIds) {
                        Entity entity = factory.getOrRegisterNewEntity(entityId);
                        if (set.remove(entity)) {
                            removed++;
                        }
                    }
                    yield removed;
                }
                default -> throw new ValidationException(
                    "Hierarchy '" + hierarchyName + "' is not an EntityList or EntitySet"
                );
            };

            dao.saveCatalog(catalog);
            logger.info("Successfully removed {} entity IDs from hierarchy {}", count, hierarchyName);
            return count;
        } catch (SQLException e) {
            logger.error("Failed to remove entity IDs from hierarchy");
            throw new CheapException("Failed to remove entity IDs: " + e.getMessage(), e);
        }
    }

    // ========================================
    // Entity Directory Mutation Operations
    // ========================================

    /**
     * Adds entries (name -> entity ID pairs) to an EntityDirectory hierarchy.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entries the map of name -> entity ID pairs to add
     * @return the number of entries added
     * @throws ResourceNotFoundException if catalog or hierarchy is not found
     * @throws ValidationException if hierarchy is not an EntityDirectory
     */
    @Transactional
    public int addDirectoryEntries(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull Map<String, UUID> entries)
    {
        logger.debug("Adding {} entries to directory {} in catalog {}", entries.size(), hierarchyName, catalogId);

        Catalog catalog = catalogService.getCatalog(catalogId);
        Hierarchy hierarchy = service.getHierarchy(catalogId, hierarchyName);

        if (!(hierarchy instanceof EntityDirectoryHierarchy directory)) {
            throw new ValidationException("Hierarchy '" + hierarchyName + "' is not an EntityDirectory");
        }

        try {
            for (Map.Entry<String, UUID> entry : entries.entrySet()) {
                Entity entity = factory.getOrRegisterNewEntity(entry.getValue());
                directory.put(entry.getKey(), entity);
            }

            dao.saveCatalog(catalog);
            logger.info("Successfully added {} entries to directory {}", entries.size(), hierarchyName);
            return entries.size();
        } catch (SQLException e) {
            logger.error("Failed to add entries to directory");
            throw new CheapException("Failed to add entries: " + e.getMessage(), e);
        }
    }

    /**
     * Removes entries from an EntityDirectory hierarchy by names.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param names the list of names to remove
     * @return the number of entries removed
     * @throws ResourceNotFoundException if catalog or hierarchy is not found
     * @throws ValidationException if hierarchy is not an EntityDirectory
     */
    @Transactional
    public int removeDirectoryEntriesByNames(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<String> names)
    {
        logger.debug("Removing {} entries by name from directory {} in catalog {}", names.size(), hierarchyName, catalogId);

        Catalog catalog = catalogService.getCatalog(catalogId);
        Hierarchy hierarchy = service.getHierarchy(catalogId, hierarchyName);

        if (!(hierarchy instanceof EntityDirectoryHierarchy directory)) {
            throw new ValidationException("Hierarchy '" + hierarchyName + "' is not an EntityDirectory");
        }

        try {
            int removed = 0;
            for (String name : names) {
                if (directory.remove(name) != null) {
                    removed++;
                }
            }

            dao.saveCatalog(catalog);
            logger.info("Successfully removed {} entries from directory {}", removed, hierarchyName);
            return removed;
        } catch (SQLException e) {
            logger.error("Failed to remove entries from directory");
            throw new CheapException("Failed to remove entries: " + e.getMessage(), e);
        }
    }

    /**
     * Removes entries from an EntityDirectory hierarchy by entity IDs.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityIds the list of entity IDs to remove
     * @return the number of entries removed
     * @throws ResourceNotFoundException if catalog or hierarchy is not found
     * @throws ValidationException if hierarchy is not an EntityDirectory
     */
    @Transactional
    public int removeDirectoryEntriesByIds(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<UUID> entityIds)
    {
        logger.debug("Removing {} entries by entity ID from directory {} in catalog {}", entityIds.size(), hierarchyName, catalogId);

        Catalog catalog = catalogService.getCatalog(catalogId);
        Hierarchy hierarchy = service.getHierarchy(catalogId, hierarchyName);

        if (!(hierarchy instanceof EntityDirectoryHierarchy directory)) {
            throw new ValidationException("Hierarchy '" + hierarchyName + "' is not an EntityDirectory");
        }

        try {
            int removed = 0;
            // Find and remove entries that have the specified entity IDs
            List<String> keysToRemove = new ArrayList<>();
            for (Map.Entry<String, Entity> entry : directory.entrySet()) {
                if (entityIds.contains(entry.getValue().globalId())) {
                    keysToRemove.add(entry.getKey());
                }
            }

            for (String key : keysToRemove) {
                if (directory.remove(key) != null) {
                    removed++;
                }
            }

            dao.saveCatalog(catalog);
            logger.info("Successfully removed {} entries from directory {}", removed, hierarchyName);
            return removed;
        } catch (SQLException e) {
            logger.error("Failed to remove entries from directory");
            throw new CheapException("Failed to remove entries: " + e.getMessage(), e);
        }
    }

    // ========================================
    // Entity Tree Mutation Operations
    // ========================================

    /**
     * Adds child nodes to an EntityTree hierarchy under a specific parent node.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param parentPath the path to the parent node
     * @param nodes the map of child name -> entity ID pairs to add
     * @return the number of nodes added
     * @throws ResourceNotFoundException if catalog, hierarchy, or parent node is not found
     * @throws ValidationException if hierarchy is not an EntityTree
     */
    @Transactional
    public int addTreeNodes(@NotNull UUID catalogId, @NotNull String hierarchyName, String parentPath, @NotNull Map<String, UUID> nodes)
    {
        logger.debug("Adding {} nodes to tree {} under path {} in catalog {}", nodes.size(), hierarchyName, parentPath, catalogId);
        parentPath = (parentPath == null) ? "/" : parentPath;

        Catalog catalog = catalogService.getCatalog(catalogId);
        Hierarchy hierarchy = service.getHierarchy(catalogId, hierarchyName);

        if (!(hierarchy instanceof EntityTreeHierarchy tree)) {
            throw new ValidationException("Hierarchy '" + hierarchyName + "' is not an EntityTree");
        }

        try {
            // Find the parent node
            EntityTreeHierarchy.Node parent = findNodeByPath(tree.root(), parentPath);
            if (parent == null) {
                throw new ResourceNotFoundException("Parent node not found at path: " + parentPath);
            }

            // Add each child node
            for (Map.Entry<String, UUID> entry : nodes.entrySet()) {
                Entity entity = factory.getOrRegisterNewEntity(entry.getValue());
                // Create a new node with the entity
                EntityTreeHierarchy.Node child = factory.createTreeNode(entity, parent);
                parent.put(entry.getKey(), child);
            }

            dao.saveCatalog(catalog);
            logger.info("Successfully added {} nodes to tree {}", nodes.size(), hierarchyName);
            return nodes.size();
        } catch (SQLException e) {
            logger.error("Failed to add nodes to tree");
            throw new CheapException("Failed to add nodes: " + e.getMessage(), e);
        }
    }

    /**
     * Removes nodes from an EntityTree hierarchy by paths.
     * Removal cascades to remove all descendants.
     *
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param paths the list of node paths to remove
     * @return the number of nodes removed (including descendants)
     * @throws ResourceNotFoundException if catalog or hierarchy is not found
     * @throws ValidationException if hierarchy is not an EntityTree
     */
    @Transactional
    public int removeTreeNodes(@NotNull UUID catalogId, @NotNull String hierarchyName, @NotNull List<String> paths)
    {
        logger.debug("Removing {} nodes from tree {} in catalog {}", paths.size(), hierarchyName, catalogId);

        Catalog catalog = catalogService.getCatalog(catalogId);
        Hierarchy hierarchy = service.getHierarchy(catalogId, hierarchyName);

        if (!(hierarchy instanceof EntityTreeHierarchy tree)) {
            throw new ValidationException("Hierarchy '" + hierarchyName + "' is not an EntityTree");
        }

        try {
            int totalRemoved = 0;
            for (String path : paths) {
                // Parse the path to get parent and child name
                String[] parts = path.split("/");
                if (parts.length == 0) {
                    continue; // Skip invalid paths
                }

                // Find parent path
                StringBuilder parentPathBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (!parts[i].isEmpty()) {
                        parentPathBuilder.append("/").append(parts[i]);
                    }
                }
                String parentPath = parentPathBuilder.toString();
                if (parentPath.isEmpty()) {
                    parentPath = "/";
                }

                String childName = parts[parts.length - 1];
                if (childName.isEmpty()) {
                    continue; // Skip invalid paths
                }

                // Find the parent node and remove the child
                EntityTreeHierarchy.Node parent = findNodeByPath(tree.root(), parentPath);
                if (parent != null) {
                    EntityTreeHierarchy.Node node = parent.get(childName);
                    if (node != null) {
                        // Count nodes before removal (node + all descendants)
                        int nodeCount = countTreeNodes(node);

                        // Remove the node (this cascades to descendants)
                        if (parent.remove(childName) != null) {
                            totalRemoved += nodeCount;
                        }
                    }
                }
            }

            dao.saveCatalog(catalog);
            logger.info("Successfully removed {} nodes (including descendants) from tree {}", totalRemoved, hierarchyName);
            return totalRemoved;
        } catch (SQLException e) {
            logger.error("Failed to remove nodes from tree");
            throw new CheapException("Failed to remove nodes: " + e.getMessage(), e);
        }
    }

    /**
     * Counts the number of nodes in a tree (node + all descendants).
     */
    private int countTreeNodes(EntityTreeHierarchy.Node node)
    {
        int count = 1; // Count this node
        for (EntityTreeHierarchy.Node child : node.values()) {
            count += countTreeNodes(child);
        }
        return count;
    }

    /**
     * Finds a node by path in the tree.
     *
     * @param root the root node
     * @param path the path to search for (e.g., "/folder1/folder2")
     * @return the node at the path, or null if not found
     */
    private EntityTreeHierarchy.Node findNodeByPath(EntityTreeHierarchy.Node root, String path)
    {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // Handle root path
        if (path.equals("/")) {
            return root;
        }

        // Split path and navigate
        String[] parts = path.split("/");
        EntityTreeHierarchy.Node current = root;

        for (String part : parts) {
            if (part.isEmpty()) {
                continue; // Skip empty parts from leading/trailing slashes
            }

            EntityTreeHierarchy.Node child = current.get(part);
            if (child == null) {
                return null; // Path not found
            }
            current = child;
        }

        return current;
    }
}
