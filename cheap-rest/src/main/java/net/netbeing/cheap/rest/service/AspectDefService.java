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
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.rest.exception.ResourceConflictException;
import net.netbeing.cheap.rest.exception.ResourceNotFoundException;
import net.netbeing.cheap.rest.exception.ValidationException;
import net.netbeing.cheap.util.CheapException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for AspectDef operations.
 * Provides business logic for creating and managing aspect definitions.
 */
@Service
public class AspectDefService
{
    private static final Logger logger = LoggerFactory.getLogger(AspectDefService.class);

    private final CheapDao dao;
    private final CheapFactory factory;

    public AspectDefService(CheapDao dao, CheapFactory factory)
    {
        this.dao = dao;
        this.factory = factory;
    }

    /**
     * Creates a new AspectDef in a catalog.
     *
     * @param catalogId the catalog ID
     * @param aspectDef the aspect definition to create
     * @return the created AspectDef (with generated UUID if not provided)
     * @throws ValidationException       if validation fails
     * @throws ResourceNotFoundException if catalog is not found
     * @throws ResourceConflictException if AspectDef with same name or ID already exists
     */
    @Transactional
    public AspectDef createAspectDef(@NotNull UUID catalogId, @NotNull AspectDef aspectDef)
    {
        if (logger.isInfoEnabled()) {
            logger.info("Creating AspectDef {} in catalog {}", aspectDef.name(), catalogId);
        }

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

        // Validate the AspectDef
        validateAspectDef(aspectDef);

        // Check for duplicate name
        for (AspectDef existing : catalog.aspectDefs()) {
            if (existing.name().equals(aspectDef.name())) {
                throw new ResourceConflictException("AspectDef with name " + aspectDef.name() + " already exists in " +
                    "catalog");
            }
            if (existing.globalId().equals(aspectDef.globalId())) {
                throw new ResourceConflictException("AspectDef with ID " + aspectDef.globalId() + " already exists in" +
                    " catalog");
            }
        }

        // Create the AspectMap hierarchy for this AspectDef
        catalog.createAspectMap(aspectDef, 0L);

        // Save the catalog (which includes the new AspectDef and hierarchy)
        try {
            dao.saveCatalog(catalog);
            logger.info("Successfully created AspectDef {} in catalog {}", aspectDef.name(), catalogId);
            return aspectDef;
        } catch (SQLException e) {
            logger.error("Failed to save catalog with new AspectDef");
            throw new CheapException("Failed to save AspectDef: " + e.getMessage(), e);
        }
    }

    /**
     * Validates an AspectDef.
     *
     * @param aspectDef the aspect definition to validate
     * @throws ValidationException if validation fails
     */
    @SuppressWarnings({"ConstantExpression", "java:S2583"})
    public void validateAspectDef(@NotNull AspectDef aspectDef)
    {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        if (aspectDef == null) {
            throw new ValidationException("AspectDef cannot be null");
        }

        // Validate name
        if (aspectDef.name().trim().isEmpty()) {
            errors.add(new ValidationException.ValidationError("name", "AspectDef name cannot be null or empty"));
        }

        // Validate that at least one property exists
        if (!aspectDef.propertyDefs().iterator().hasNext()) {
            errors.add(new ValidationException.ValidationError("properties", "AspectDef must have at least one " +
                "property"));
        }

        // Validate property names are unique
        Set<String> propertyNames = new HashSet<>();
        int propertyIndex = 0;
        for (PropertyDef propertyDef : aspectDef.propertyDefs()) {
            if (propertyDef == null) {
                errors.add(new ValidationException.ValidationError("properties[" + propertyIndex + "]", "PropertyDef " +
                    "cannot be null"));
                propertyIndex++;
                continue;
            }

            if (propertyDef.name().trim().isEmpty()) {
                errors.add(new ValidationException.ValidationError("properties[" + propertyIndex + "].name",
                    "PropertyDef name cannot be null or empty"));
            } else {
                if (!propertyNames.add(propertyDef.name())) {
                    errors.add(new ValidationException.ValidationError("properties[" + propertyIndex + "].name",
                        "Duplicate property name: " + propertyDef.name()));
                }
            }

            propertyIndex++;
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("AspectDef validation failed", errors);
        }
    }
}
