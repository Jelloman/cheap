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
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
import net.netbeing.cheap.rest.exception.ResourceNotFoundException;
import net.netbeing.cheap.rest.exception.ValidationException;
import net.netbeing.cheap.util.CheapException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service layer for Catalog operations.
 * Provides business logic for creating and managing catalogs.
 */
@Service
public class CatalogService
{
    private static final Logger logger = LoggerFactory.getLogger(CatalogService.class);

    private final CheapDao dao;
    private final CheapFactory factory;
    private final DataSource dataSource;

    private final ConcurrentMap<UUID, Catalog> catalogStore = new ConcurrentHashMap<>();

    @Resource
    @Lazy
    private CatalogService service;

    public CatalogService(CheapDao dao, CheapFactory factory, DataSource dataSource)
    {
        this.dao = dao;
        this.factory = factory;
        this.dataSource = dataSource;
    }

    /**
     * Sets the service reference for Spring proxy injection.
     * Package-private for testing purposes.
     *
     * @param service the service reference
     */
    void setService(CatalogService service)
    {
        this.service = service;
    }

    /**
     * Creates a new catalog from a CatalogDef.
     *
     * @param catalogDef the catalog definition
     * @param species the catalog species
     * @param upstream the upstream catalog ID (may be null for SOURCE/SINK)
     * @param baseCatalogURL optional URI for the catalog
     * @return the UUID of the newly created catalog
     * @throws ValidationException if validation fails
     */
    @Transactional
    public UUID createCatalog(@NotNull CatalogDef catalogDef, @NotNull CatalogSpecies species,
                              UUID upstream, URI baseCatalogURL)
    {
        logger.info("Creating catalog with species: {}", species);

        // Validate the catalog definition
        validateCatalogDef(catalogDef);

        // Validate species and upstream combination
        validateSpeciesUpstream(species, upstream);

        // Create a new catalog with a new UUID
        UUID catalogId = UUID.randomUUID();
        URI catalogURI = URI.create(baseCatalogURL.toString() + "/" + catalogId);
        Catalog catalog = factory.createCatalog(catalogId, species, catalogURI, upstream, 0L);

        // Add all hierarchies from the CatalogDef
        for (HierarchyDef hierarchyDef : catalogDef.hierarchyDefs()) {
            createAndAddHierarchy(catalog, hierarchyDef);
        }

        // Add all AspectDefs and auto-create AspectMap hierarchies
        for (AspectDef aspectDef : catalogDef.aspectDefs()) {
            // Create the AspectMap hierarchy for this AspectDef
            catalog.createAspectMap(aspectDef, 0L);
        }

        // Save the catalog
        try {
            logger.info("Creating catalog with ID: {}", catalogId);
            dao.saveCatalog(catalog);
            logger.info("Successfully created catalog with ID: {}", catalogId);

            catalogStore.put(catalogId, catalog);

            return catalogId;
        } catch (SQLException e) {
            logger.error("Failed to save catalog");
            throw new CheapException("Failed to save catalog: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all catalog IDs with pagination.
     *
     * @param page the page number (zero-indexed)
     * @param size the page size
     * @return list of catalog IDs for the requested page
     */
    @Transactional(readOnly = true)
    public List<UUID> listCatalogIds(int page, int size)
    {
        logger.debug("Listing catalog IDs - page: {}, size: {}", page, size);

        try (Connection conn = dataSource.getConnection()) {
            // Query all catalog IDs from the database
            List<UUID> allCatalogIds = new ArrayList<>();
            String sql = "SELECT catalog_id FROM catalog ORDER BY catalog_id"; // FIXME!!

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String catalogIdStr = rs.getString("catalog_id");
                    allCatalogIds.add(UUID.fromString(catalogIdStr));
                }
            }

            // Calculate pagination
            int start = page * size;
            int end = Math.min(start + size, allCatalogIds.size());

            if (start >= allCatalogIds.size()) {
                return new ArrayList<>();
            }

            return new ArrayList<>(allCatalogIds.subList(start, end));
        } catch (SQLException e) {
            logger.error("Failed to list catalog IDs");
            throw new CheapException("Failed to list catalog IDs: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the total count of catalogs.
     *
     * @return total number of catalogs
     */
    @Transactional(readOnly = true)
    public long countCatalogs()
    {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT COUNT(*) FROM catalog";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            logger.error("Failed to count catalogs");
            throw new CheapException("Failed to count catalogs: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a catalog by ID.
     *
     * @param catalogId the catalog ID
     * @return the catalog
     * @throws ResourceNotFoundException if catalog is not found
     */
    @Transactional(readOnly = true)
    public Catalog getCatalog(@NotNull UUID catalogId)
    {
        logger.debug("Getting catalog: {}", catalogId);

        Catalog catalog = catalogStore.get(catalogId);
        if (catalog != null) {
            return catalog;
        }

        try {
            catalog = dao.loadCatalog(catalogId);
            if (catalog == null) {
                throw new ResourceNotFoundException("Catalog not found: " + catalogId);
            }
            catalogStore.put(catalogId, catalog);
            return catalog;
        } catch (SQLException e) {
            logger.error("Failed to load catalog");
            throw new CheapException("Failed to load catalog: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a catalog definition by catalog ID.
     *
     * @param catalogId the catalog ID
     * @return the catalog definition
     * @throws ResourceNotFoundException if catalog is not found
     */
    @Transactional(readOnly = true)
    public CatalogDef getCatalogDef(@NotNull UUID catalogId)
    {
        Catalog catalog = service.getCatalog(catalogId);

        // Convert Hierarchies to HierarchyDefs
        List<HierarchyDef> hierarchyDefs = new ArrayList<>();
        for (Hierarchy hierarchy : catalog.hierarchies()) {
            // Skip AspectMap hierarchies as they are not part of the CatalogDef
            if (!(hierarchy instanceof AspectMapHierarchy)) {
                hierarchyDefs.add(factory.createHierarchyDef(hierarchy.name(), hierarchy.type()));
            }
        }

        // Get AspectDefs from catalog
        List<AspectDef> aspectDefs = new ArrayList<>();
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            aspectDefs.add(aspectDef);
        }

        return factory.createCatalogDef(hierarchyDefs, aspectDefs);
    }

    /**
     * Validates a CatalogDef.
     *
     * @param catalogDef the catalog definition to validate
     * @throws ValidationException if validation fails
     */
    @SuppressWarnings("java:S2583")
    public void validateCatalogDef(@NotNull CatalogDef catalogDef)
    {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        if (catalogDef == null) {
            throw new ValidationException("CatalogDef cannot be null");
        }

        // Validate HierarchyDefs
        validateHierarchyDefs(catalogDef, errors);

        // Validate AspectDefs
        validateAspectDefs(catalogDef, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException("CatalogDef validation failed", errors);
        }
    }

    private static void validateAspectDefs(@NotNull CatalogDef catalogDef, List<ValidationException.ValidationError> errors)
    {
        Set<String> aspectDefNames = new HashSet<>();
        Set<UUID> aspectDefIds = new HashSet<>();
        int aspectIndex = 0;
        for (AspectDef aspectDef : catalogDef.aspectDefs()) {
            if (aspectDef == null) {
                errors.add(new ValidationException.ValidationError(
                    "catalogDef.aspectDefs[" + aspectIndex + "]",
                    "AspectDef cannot be null"
                ));
                aspectIndex++;
                continue;
            }

            if (aspectDef.name().trim().isEmpty()) {
                errors.add(new ValidationException.ValidationError(
                    "catalogDef.aspectDefs[" + aspectIndex + "].name",
                    "AspectDef name cannot be null or empty"
                ));
            } else {
                if (!aspectDefNames.add(aspectDef.name())) {
                    errors.add(new ValidationException.ValidationError(
                        "catalogDef.aspectDefs[" + aspectIndex + "].name",
                        "Duplicate AspectDef name: " + aspectDef.name()
                    ));
                }
            }

            if (!aspectDefIds.add(aspectDef.globalId())) {
                errors.add(new ValidationException.ValidationError(
                    "catalogDef.aspectDefs[" + aspectIndex + "].id",
                    "Duplicate AspectDef ID: " + aspectDef.globalId()
                ));
            }

            // Validate that AspectDef has at least one property
            if (!aspectDef.propertyDefs().iterator().hasNext()) {
                errors.add(new ValidationException.ValidationError(
                    "catalogDef.aspectDefs[" + aspectIndex + "].properties",
                    "AspectDef must have at least one property"
                ));
            }

            aspectIndex++;
        }
    }

    private static void validateHierarchyDefs(@NotNull CatalogDef catalogDef, List<ValidationException.ValidationError> errors)
    {
        Set<String> hierarchyNames = new HashSet<>();
        int hierarchyIndex = 0;
        for (HierarchyDef hierarchyDef : catalogDef.hierarchyDefs()) {
            if (hierarchyDef == null) {
                errors.add(new ValidationException.ValidationError(
                    "catalogDef.hierarchyDefs[" + hierarchyIndex + "]",
                    "HierarchyDef cannot be null"
                ));
                hierarchyIndex++;
                continue;
            }

            if (hierarchyDef.name() == null || hierarchyDef.name().trim().isEmpty()) {
                errors.add(new ValidationException.ValidationError(
                    "catalogDef.hierarchyDefs[" + hierarchyIndex + "].name",
                    "HierarchyDef name cannot be null or empty"
                ));
            } else {
                if (!hierarchyNames.add(hierarchyDef.name())) {
                    errors.add(new ValidationException.ValidationError(
                        "catalogDef.hierarchyDefs[" + hierarchyIndex + "].name",
                        "Duplicate HierarchyDef name: " + hierarchyDef.name()
                    ));
                }
            }

            if (hierarchyDef.type() == null) {
                errors.add(new ValidationException.ValidationError(
                    "catalogDef.hierarchyDefs[" + hierarchyIndex + "].type",
                    "HierarchyDef type cannot be null"
                ));
            }

            // Do not allow AspectMap hierarchies in the hierarchyDefs - they are auto-created
            if (hierarchyDef.type() == HierarchyType.ASPECT_MAP) {
                errors.add(new ValidationException.ValidationError(
                    "catalogDef.hierarchyDefs[" + hierarchyIndex + "].type",
                    "AspectMap hierarchies are auto-created from AspectDefs and should not be in hierarchyDefs"
                ));
            }

            hierarchyIndex++;
        }
    }

    /**
     * Validates the combination of species and upstream.
     *
     * @param species the catalog species
     * @param upstream the upstream catalog ID (may be null)
     * @throws ValidationException if the combination is invalid
     */
    private void validateSpeciesUpstream(@NotNull CatalogSpecies species, UUID upstream)
    {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        switch (species) {
            case SOURCE, SINK, FORK -> {
                if (upstream != null) {
                    errors.add(new ValidationException.ValidationError(
                        "upstream",
                        species + " catalogs must not have an upstream catalog"
                    ));
                }
            }
            case MIRROR, CACHE, CLONE -> {
                if (upstream == null) {
                    errors.add(new ValidationException.ValidationError(
                        "upstream",
                        species + " catalogs must have an upstream catalog"
                    ));
                }
                // Validating that the upstream catalog exists is not in scope here.
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Species/upstream validation failed", errors);
        }
    }

    /**
     * Creates a hierarchy from a HierarchyDef and adds it to the catalog.
     *
     * @param catalog the catalog to add the hierarchy to
     * @param hierarchyDef the hierarchy definition
     */
    protected void createAndAddHierarchy(@NotNull Catalog catalog, @NotNull HierarchyDef hierarchyDef)
    {
        switch (hierarchyDef.type()) {
            case ENTITY_LIST -> catalog.createEntityList(hierarchyDef.name(), 0L);
            case ENTITY_SET -> catalog.createEntitySet(hierarchyDef.name(), 0L);
            case ENTITY_DIR -> catalog.createEntityDirectory(hierarchyDef.name(), 0L);
            case ENTITY_TREE -> catalog.createEntityTree(hierarchyDef.name(), null, 0L);
            case ASPECT_MAP -> throw new IllegalStateException(
                "AspectMap hierarchies should not be in hierarchyDefs"
            );
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Created hierarchy: {} of type {}", hierarchyDef.name(), hierarchyDef.type());
        }
    }
}
