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

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyType;
import org.jetbrains.annotations.NotNull;

/**
 * Basic implementation of an AspectMapHierarchy that maps entities to aspects.
 * This hierarchy type stores a mapping from entity IDs to aspects of a single type.
 * <p>
 * This class uses composition with an internal LinkedHashMap to provide efficient
 * entity-to-aspect lookups while implementing the {@link AspectMapHierarchy} interface.
 *
 * @see AspectMapHierarchy
 * @see Hierarchy
 */
public class AspectMapHierarchyImpl extends AspectMapImpl implements AspectMapHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The name of this hierarchy in the catalog. */
    private final String name;

    /** The version number of this hierarchy. */
    private final long version;

    /**
     * Creates a new AspectMapHierarchyImpl to contain the given AspectDef.
     * Package-private for use by CatalogImpl factory methods.
     *
     * @param aspectDef the aspect definition for aspects in this hierarchy
     */
    protected AspectMapHierarchyImpl(@NotNull Catalog catalog, @NotNull AspectDef aspectDef)
    {
        this(catalog, aspectDef, 0L);
    }

    /**
     * Creates a new AspectMapHierarchyImpl to contain the given AspectDef with version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @param version the version number of this hierarchy
     */
    protected AspectMapHierarchyImpl(@NotNull Catalog catalog, @NotNull AspectDef aspectDef, long version)
    {
        super(aspectDef);
        this.catalog = catalog;
        this.version = version;
        this.name = aspectDef.name();
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
