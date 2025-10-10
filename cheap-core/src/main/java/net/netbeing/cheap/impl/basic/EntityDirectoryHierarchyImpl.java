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

package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

/**
 * Basic implementation of an EntityDirectoryHierarchy using a HashMap.
 * This hierarchy type represents a string-to-entity mapping, corresponding
 * to the ENTITY_DIR (ED) hierarchy type in Cheap.
 * <p>
 * This implementation extends HashMap to provide efficient name-based entity
 * lookup while implementing the EntityDirectoryHierarchy interface.
 * 
 * @see EntityDirectoryHierarchy
 * @see Entity
 * @see HierarchyDef
 */
public class EntityDirectoryHierarchyImpl extends LinkedHashMap<String, Entity> implements EntityDirectoryHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The name of this hierarchy in the catalog. */
    private final String name;

    /** The version number of this hierarchy. */
    private final long version;

    /**
     * Creates a new EntityDirectoryHierarchyImpl with the specified hierarchy definition.
     *
     * @param catalog the owning catalog
     * @param name the name of this hierarchy in the catalog
     */
    public EntityDirectoryHierarchyImpl(@NotNull Catalog catalog, @NotNull String name)
    {
        this(catalog, name, 0L);
    }

    /**
     * Creates a new EntityDirectoryHierarchyImpl with the specified hierarchy definition and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param version the version number of this hierarchy
     */
    public EntityDirectoryHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, long version)
    {
        this.catalog = catalog;
        this.name = name;
        this.version = version;
        catalog.addHierarchy(this);
    }

    /**
     * Returns the Catalog that owns this hierarchy.
     *
     * @return the parent catalog
     */
    @Override
    public @NotNull Catalog catalog()
    {
        return catalog;
    }

    /**
     * Returns the name of this hierarchy in the catalog.
     *
     * @return the name of the hierarchy
     */
    @Override
    public @NotNull String name()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull HierarchyType type()
    {
        return HierarchyType.ENTITY_DIR;
    }

    /**
     * Returns the version number of this hierarchy.
     *
     * @return the version number
     */
    @Override
    public long version()
    {
        return version;
    }
}
