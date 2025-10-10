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

import java.util.HashMap;

/**
 * Basic implementation of an AspectMapHierarchy that maps entities to aspects.
 * This hierarchy type stores a mapping from entity IDs to aspects of a single type.
 * <p>
 * This class extends {@link HashMap} to provide efficient entity-to-aspect lookups
 * while implementing the {@link AspectMapHierarchy} interface.
 * 
 * @see AspectMapHierarchy
 * @see Hierarchy
 */
public class AspectMapHierarchyImpl extends HashMap<Entity, Aspect> implements AspectMapHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The name of this hierarchy in the catalog. */
    private final String name;

    /** The aspect definition for the aspects stored in this hierarchy. */
    private final AspectDef aspectDef;

    /** The version number of this hierarchy. */
    private final long version;

    /**
     * Creates a new AspectMapHierarchyImpl to contain the given AspectDef.
     * AA new HierarchyDef will be constructed.
     *
     * @param aspectDef the aspect definition for aspects in this hierarchy
     */
    public AspectMapHierarchyImpl(@NotNull Catalog catalog, @NotNull AspectDef aspectDef)
    {
        this(catalog, aspectDef, 0L);
    }

    /**
     * Creates a new AspectMapHierarchyImpl to contain the given AspectDef with version.
     * A new HierarchyDef will be constructed.
     *
     * @param catalog the catalog containing this hierarchy
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @param version the version number of this hierarchy
     */
    public AspectMapHierarchyImpl(@NotNull Catalog catalog, @NotNull AspectDef aspectDef, long version)
    {
        this.catalog = catalog;
        this.aspectDef = aspectDef;
        this.version = version;
        this.name = aspectDef.name();
        catalog.addHierarchy(this);
    }

    /**
     * Returns the aspect definition for aspects stored in this hierarchy.
     * 
     * @return the aspect definition
     */
    @Override
    public AspectDef aspectDef()
    {
        return aspectDef;
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
        return HierarchyType.ASPECT_MAP;
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
