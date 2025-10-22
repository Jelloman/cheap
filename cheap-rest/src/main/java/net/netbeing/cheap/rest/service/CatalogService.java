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
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
import net.netbeing.cheap.rest.exception.ValidationException;
import net.netbeing.cheap.util.CheapException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    public CatalogService(CheapDao dao, CheapFactory factory)
    {
        this.dao = dao;
        this.factory = factory;
    }

    /**
     * Creates a new catalog from a CatalogDef.
     *
     * @param catalogDef the catalog definition
     * @param species the catalog species
     * @param upstream the upstream catalog ID (may be null for SOURCE/SINK)
     * @param uri optional URI for the catalog
     * @return the UUID of the newly created catalog
     * @throws ValidationException if validation fails
     */
    @Transactional
    public UUID createCatalog(@NotNull CatalogDef catalogDef, @NotNull CatalogSpecies species,
                              UUID upstream, URI uri)
    {
        logger.info("Creating catalog with species: {}", species);

        // Validate the catalog definition
        validateCatalogDef(catalogDef);

        // Validate species and upstream combination
        validateSpeciesUpstream(species, upstream);

        // Create a new catalog with a new UUID
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, species, uri, upstream, 0L);

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
            dao.saveCatalog(catalog);
            logger.info("Successfully created catalog with ID: {}", catalogId);
            return catalogId;
        } catch (SQLException e) {
            logger.error("Failed to save catalog");
            throw new CheapException("Failed to save catalog: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a CatalogDef.
     *
     * @param catalogDef the catalog definition to validate
     * @throws ValidationException if validation fails
     */
    public void validateCatalogDef(@NotNull CatalogDef catalogDef)
    {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        if (catalogDef == null) {
            throw new ValidationException("CatalogDef cannot be null");
        }

        // Validate HierarchyDefs
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

        // Validate AspectDefs
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

        if (!errors.isEmpty()) {
            throw new ValidationException("CatalogDef validation failed", errors);
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
                // TODO: Validate that upstream catalog exists
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
    private void createAndAddHierarchy(@NotNull Catalog catalog, @NotNull HierarchyDef hierarchyDef)
    {
        switch (hierarchyDef.type()) {
            case ENTITY_LIST -> catalog.createEntityList(hierarchyDef.name(), 0L);
            case ENTITY_SET -> catalog.createEntitySet(hierarchyDef.name(), 0L);
            case ENTITY_DIR -> catalog.createEntityDirectory(hierarchyDef.name(), 0L);
            case ENTITY_TREE -> catalog.createEntityTree(hierarchyDef.name(), null, 0L);
            case ASPECT_MAP -> throw new IllegalStateException(
                "AspectMap hierarchies should not be in hierarchyDefs"
            );
        };

        if (logger.isDebugEnabled()) {
            logger.debug("Created hierarchy: {} of type {}", hierarchyDef.name(), hierarchyDef.type());
        }
    }
}
