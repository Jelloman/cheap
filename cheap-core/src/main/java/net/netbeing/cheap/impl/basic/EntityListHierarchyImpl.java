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

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityListHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Basic implementation of an EntityListHierarchy using an ArrayList.
 * This hierarchy type represents an ordered list of entities that may contain
 * duplicates, corresponding to the ENTITY_LIST (EL) hierarchy type in Cheap.
 * <p>
 * This implementation extends ArrayList to provide indexed access and maintain
 * insertion order while implementing the EntityListHierarchy interface.
 * 
 * @see EntityListHierarchy
 * @see Entity
 * @see HierarchyDef
 */
public class EntityListHierarchyImpl extends ArrayList<Entity> implements EntityListHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The name of this hierarchy in the catalog. */
    private final String name;

    /** The version number of this hierarchy. */
    private final long version;

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     */
    public EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull String name)
    {
        this(catalog, name, 0L);
    }

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition and
     * initial capacity.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param initialCapacity initial capacity of list
     */
    public EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, int initialCapacity)
    {
        this(catalog, name, initialCapacity, 0L);
    }

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param version the version number of this hierarchy
     */
    public EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, long version)
    {
        this.catalog = catalog;
        this.name = name;
        this.version = version;
        catalog.addHierarchy(this);
    }

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition,
     * initial capacity, and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param initialCapacity initial capacity of list
     * @param version the version number of this hierarchy
     */
    public EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, int initialCapacity, long version)
    {
        super(initialCapacity);
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
        return HierarchyType.ENTITY_LIST;
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
