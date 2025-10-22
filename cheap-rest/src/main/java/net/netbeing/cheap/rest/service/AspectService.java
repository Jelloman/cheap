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
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.rest.exception.ResourceNotFoundException;
import net.netbeing.cheap.rest.exception.UnprocessableEntityException;
import net.netbeing.cheap.rest.exception.ValidationException;
import net.netbeing.cheap.util.CheapException;
import net.netbeing.cheap.util.PropertyValueAdapter;
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
 * Service layer for Aspect operations.
 * Provides business logic for upserting and querying aspects.
 */
@Service
public class AspectService
{
    private static final Logger logger = LoggerFactory.getLogger(AspectService.class);

    private final CheapDao dao;
    private final CheapFactory factory;
    private final PropertyValueAdapter propertyAdapter;

    public AspectService(CheapDao dao, CheapFactory factory)
    {
        this.dao = dao;
        this.factory = factory;
        this.propertyAdapter = new PropertyValueAdapter();
    }

    /**
     * Result of an aspect upsert operation for a single entity.
     */
    public record UpsertResult(
        UUID entityId,
        boolean success,
        boolean created,
        String message
    )
    {
    }

    /**
     * Upserts aspects for multiple entities.
     *
     * @param catalogId the catalog ID
     * @param aspectDefName the AspectDef name
     * @param aspectsByEntity map of entity ID to property values
     * @param createEntities whether to auto-create entities that don't exist
     * @return map of entity ID to upsert result
     * @throws ValidationException if validation fails
     * @throws ResourceNotFoundException if catalog or AspectDef is not found
     * @throws UnprocessableEntityException if entities don't exist and createEntities is false
     */
    @Transactional
    public Map<UUID, UpsertResult> upsertAspects(@NotNull UUID catalogId, @NotNull String aspectDefName,
                                                 @NotNull Map<UUID, Map<String, Object>> aspectsByEntity,
                                                 boolean createEntities)
    {
        logger.info("Upserting {} aspects of type {} in catalog {}",
            aspectsByEntity.size(), aspectDefName, catalogId);

        // Load the catalog
        Catalog catalog;
        try {
            catalog = dao.loadCatalog(catalogId);
            if (catalog == null) {
                throw new ResourceNotFoundException("Catalog not found: " + catalogId);
            }
        } catch (SQLException e) {
            logger.error("Failed to load catalog");
            throw new CheapException("Failed to load catalog: " + e.getMessage(), e);
        }

        // Find the AspectDef
        AspectDef aspectDef = null;
        for (AspectDef def : catalog.aspectDefs()) {
            if (def.name().equals(aspectDefName)) {
                aspectDef = def;
                break;
            }
        }
        if (aspectDef == null) {
            throw new ResourceNotFoundException("AspectDef not found: " + aspectDefName);
        }

        // Get the AspectMap hierarchy
        AspectMapHierarchy aspectMap = catalog.aspects(aspectDef);
        if (aspectMap == null) {
            throw new ResourceNotFoundException("AspectMap hierarchy not found for: " + aspectDefName);
        }

        // Process each aspect
        Map<UUID, UpsertResult> results = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, Object>> entry : aspectsByEntity.entrySet()) {
            UUID entityId = entry.getKey();
            Map<String, Object> properties = entry.getValue();

            try {
                // Validate property data
                validateAspectData(aspectDef, properties);

                // Get or create the entity
                Entity entity = factory.getEntity(entityId);
                if (entity == null) {
                    if (!createEntities) {
                        results.put(entityId, new UpsertResult(
                            entityId, false, false, "Entity does not exist and createEntities is false"
                        ));
                        continue;
                    }
                    // Create new entity
                    entity = factory.createEntity(entityId);
                    factory.registerEntity(entity);
                    logger.debug("Created new entity: {}", entityId);
                }

                // Check if aspect already exists
                Aspect existingAspect = aspectMap.get(entity);
                boolean isUpdate = existingAspect != null;

                // Create the aspect
                Aspect aspect = factory.createObjectMapAspect(entity, aspectDef);

                // Set properties
                for (Map.Entry<String, Object> propEntry : properties.entrySet()) {
                    String propName = propEntry.getKey();
                    Object propValue = propEntry.getValue();

                    PropertyDef propDef = aspectDef.propertyDef(propName);
                    if (propDef == null) {
                        throw new ValidationException(
                            "Property " + propName + " is not defined in AspectDef " + aspectDefName
                        );
                    }

                    // Coerce the value to the correct type
                    Object coercedValue = propertyAdapter.coerce(propDef, propValue);
                    aspect.unsafeWrite(propName, coercedValue);
                }

                // Validate required properties are present
                for (PropertyDef propDef : aspectDef.propertyDefs()) {
                    if (!propDef.isNullable() && !properties.containsKey(propDef.name()) &&
                        (existingAspect == null || existingAspect.get(propDef.name()) == null)) {
                        throw new ValidationException(
                            "Required property " + propDef.name() + " is missing"
                        );
                    }

                }

                // Add or update the aspect in the hierarchy
                aspectMap.put(entity, aspect);

                results.put(entityId, new UpsertResult(
                    entityId, true, !isUpdate,
                    isUpdate ? "Aspect updated" : "Aspect created"
                ));

            } catch (Exception e) {
                logger.error("Failed to upsert aspect for entity {}", entityId, e);
                results.put(entityId, new UpsertResult(
                    entityId, false, false, e.getMessage()
                ));
            }
        }

        // Save the catalog with all changes
        try {
            dao.saveCatalog(catalog);
            logger.info("Successfully upserted aspects in catalog {}", catalogId);
        } catch (SQLException e) {
            logger.error("Failed to save catalog with upserted aspects");
            throw new CheapException("Failed to save aspects: " + e.getMessage(), e);
        }

        return results;
    }

    /**
     * Validates aspect data against an AspectDef.
     *
     * @param aspectDef the aspect definition
     * @param properties the property values to validate
     * @throws ValidationException if validation fails
     */
    public void validateAspectData(@NotNull AspectDef aspectDef, @NotNull Map<String, Object> properties)
    {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        if (properties == null) {
            throw new ValidationException("Properties cannot be null");
        }

        // Check for unknown properties
        for (String propName : properties.keySet()) {
            PropertyDef propDef = aspectDef.propertyDef(propName);
            if (propDef == null) {
                errors.add(new ValidationException.ValidationError(
                    propName,
                    "Property " + propName + " is not defined in AspectDef " + aspectDef.name()
                ));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Aspect data validation failed", errors);
        }
    }

    /**
     * Queries aspects for multiple entities.
     *
     * @param catalogId the catalog ID
     * @param entityIds the set of entity IDs to query
     * @param aspectDefNames the set of AspectDef names to retrieve (empty = all)
     * @return map of entity ID to map of AspectDef name to Aspect
     * @throws ResourceNotFoundException if catalog is not found
     */
    @Transactional(readOnly = true)
    public Map<UUID, Map<String, Aspect>> queryAspects(@NotNull UUID catalogId,
                                                        @NotNull java.util.Set<UUID> entityIds,
                                                        java.util.Set<String> aspectDefNames)
    {
        logger.info("Querying aspects for {} entities in catalog {}", entityIds.size(), catalogId);

        // Load the catalog
        Catalog catalog;
        try {
            catalog = dao.loadCatalog(catalogId);
            if (catalog == null) {
                throw new ResourceNotFoundException("Catalog not found: " + catalogId);
            }
        } catch (SQLException e) {
            logger.error("Failed to load catalog");
            throw new CheapException("Failed to load catalog: " + e.getMessage(), e);
        }

        Map<UUID, Map<String, Aspect>> results = new LinkedHashMap<>();

        // For each entity
        for (UUID entityId : entityIds) {
            Entity entity = factory.getEntity(entityId);
            if (entity == null) {
                // Entity doesn't exist, skip it
                continue;
            }

            Map<String, Aspect> aspectsByName = new LinkedHashMap<>();

            // Determine which AspectDefs to query
            Iterable<AspectDef> aspectDefsToQuery;
            if (aspectDefNames == null || aspectDefNames.isEmpty()) {
                // Query all AspectDefs
                aspectDefsToQuery = catalog.aspectDefs();
            } else {
                // Query only specified AspectDefs
                List<AspectDef> filtered = new ArrayList<>();
                for (AspectDef aspectDef : catalog.aspectDefs()) {
                    if (aspectDefNames.contains(aspectDef.name())) {
                        filtered.add(aspectDef);
                    }
                }
                aspectDefsToQuery = filtered;
            }

            // Get aspects for each AspectDef
            for (AspectDef aspectDef : aspectDefsToQuery) {
                AspectMapHierarchy aspectMap = catalog.aspects(aspectDef);
                if (aspectMap != null) {
                    Aspect aspect = aspectMap.get(entity);
                    if (aspect != null) {
                        aspectsByName.put(aspectDef.name(), aspect);
                    }
                }
            }

            if (!aspectsByName.isEmpty()) {
                results.put(entityId, aspectsByName);
            }
        }

        return results;
    }
}
