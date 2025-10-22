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
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.EntityDirectoryHierarchy;
import net.netbeing.cheap.model.EntityListHierarchy;
import net.netbeing.cheap.model.EntitySetHierarchy;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Full implementation of a Catalog in the Cheap architecture. A catalog represents
 * either an external data source or a mirror/clone/fork of another catalog. A catalog
 * contains hierarchies of entities and their aspects.
 *
 * @see Catalog
 * @see CatalogDef
 * @see LocalEntity
 * @see LocalEntityOneCatalogImpl
 */
public class CatalogImpl extends LocalEntityOneCatalogImpl implements Catalog
{
    /** The immutable species of this Catalog. */
    private final CatalogSpecies species;

    /** The mutable uri of this Catalog. */
    private URI uri;

    /** The upstream catalog this catalog mirrors, or null for root catalogs. */
    private final UUID upstream;

    /** The version number of this catalog. */
    private final long version;

    /** Directory of hierarchies contained in this catalog. */
    private final Map<String, Hierarchy> hierarchies;

    /** Directory of aspect definitions available in this catalog. */
    private final Map<String, AspectDef> aspectage;

    /**
     * Creates a new non-strict SINK catalog with a wrapper CatalogDef that
     * fully delegates to this catalog.
     */
    public CatalogImpl()
    {
        this(UUID.randomUUID(), CatalogSpecies.SINK, null, 0L);
    }

    /**
     * Creates a new non-strict SINK catalog with a wrapper CatalogDef that
     * fully delegates to this catalog.
     */
    public CatalogImpl(UUID globalId)
    {
        this(globalId, CatalogSpecies.SINK, null, 0L);
    }

    /**
     * Creates a new non-strict catalog with the specified species and upstream,
     * and a wrapper CatalogDef that fully delegates to this catalog.
     *
     * @throws IllegalArgumentException if a SOURCE/SINK catalog has an upstream; or for other species, if it lacks one
     */
    public CatalogImpl(CatalogSpecies species, UUID upstream)
    {
        this(UUID.randomUUID(), species, upstream, 0L);
    }

    /**
     * Creates a new catalog with the specified definition and upstream catalog.
     *
     * @param upstream the upstream catalog to mirror, or null for root catalogs
     * @param version the version number of this catalog
     * @throws IllegalArgumentException if a SOURCE/SINK catalog has an upstream; or for other species, if it lacks one
     */
    public CatalogImpl(@NotNull UUID globalId, @NotNull CatalogSpecies species, UUID upstream, long version)
    {
        super(globalId, null);
        this.catalog = this;

        switch (species) {
            case SOURCE, SINK -> {
                if (upstream != null) {
                    throw new IllegalArgumentException("Source and Sink catalogs may not have an upstream catalog.");
                }
            }
            default -> {
                if (upstream == null) {
                    throw new IllegalArgumentException(species + " catalogs must have an upstream catalog.");
                }
            }
        }

        this.species = species;
        this.upstream = upstream;
        this.version = version;

        this.hierarchies = new LinkedHashMap<>();
        this.aspectage = new LinkedHashMap<>();
    }

    /**
     * Returns the type of this catalog.
     *
     * @return the catalog type (ROOT or MIRROR)
     */
    @Override
    public @NotNull CatalogSpecies species()
    {
        return species;
    }

    /**
     * The URI of this catalog. Usually a URL, but need not be.
     * Cheap is not concerned with network layers, only modeling.
     *
     * @return the URI of this catalog
     */
    @Override
    public @NotNull URI uri()
    {
        return uri;
    }

    /**
     * Set the URI of this catalog. Usually a URL, but need not be.
     *
     * @param uri the URI of this catalog
     */
    public void uri(@NotNull URI uri)
    {
        this.uri = uri;
    }

    /**
     * Returns the upstream catalog of this catalo.
     * 
     * @return the upstream catalog, or {@code null} for SOURCE and SINK catalogs
     */
    @Override
    public UUID upstream()
    {
        return upstream;
    }

    /**
     * Returns the collection of hierarchies contained within this catalog.
     *
     * @return the hierarchy collection for this catalog, never null
     */
    @Override
    public @NotNull Iterable<Hierarchy> hierarchies()
    {
        return Collections.unmodifiableCollection(hierarchies.values());
    }

    /**
     * Returns the collection of all AspectDefs contained within this catalog.
     * This is always a superset of the AspectDef collection provided by the CatalogDef.
     *
     * @return the AspectDef collection for this catalog, never null
     */
    @Override
    public @NotNull Iterable<AspectDef> aspectDefs()
    {
        return Collections.unmodifiableCollection(aspectage.values());
    }

    /**
     * Retrieves a hierarchy by name from this catalog.
     * 
     * @param name the name of the hierarchy to retrieve
     * @return the hierarchy with the given name, or {@code null} if not found
     */
    @Override
    public Hierarchy hierarchy(@NotNull String name)
    {
        return hierarchies.get(name);
    }

    /**
     * Adds a new hierarchy to this catalog. This will replace the existing hierarchy of the
     * same name, if one exists, unless the existing hierarchy is an AspectMapHierarchy.
     *
     * @param hierarchy the hierarchy to add
     */
    @Override
    public Hierarchy addHierarchy(@NotNull Hierarchy hierarchy)
    {
        if (hierarchy.catalog() != this) {
            throw new IllegalArgumentException("Cannot add a Hierarchy to a Catalog unless the Catalog is already set as the Hierarchy's owner.");
        }
        String hName = hierarchy.name();
        Hierarchy existing = hierarchy(hName);
        if (hierarchy == existing) {
            return hierarchy;
        }
        if (existing instanceof AspectMapHierarchy) {
            throw new UnsupportedOperationException("A hierarchy may not be added to a Catalog with the same name as an existing AspectMapHierarchy.");
        }
        if (hierarchy instanceof AspectMapHierarchy amh) {
            AspectDef aspectDef = amh.aspectDef();
            aspectage.put(aspectDef.name(), aspectDef);
        }
        return hierarchies.put(hName, hierarchy);
    }

    @Override
    public boolean containsAspects(@NotNull String name)
    {
        return hierarchies.get(name) instanceof AspectMapHierarchy;
    }

    /**
     * Returns the version number of this catalog.
     *
     * @return the version number
     */
    @Override
    public long version()
    {
        return version;
    }

    /**
     * Creates a new EntityListHierarchy with the specified name and adds it to this catalog.
     *
     * @param name the name of the hierarchy to create
     * @return the newly created EntityListHierarchy
     */
    @Override
    public EntityListHierarchy createEntityList(@NotNull String name, long version, int initialCapacity)
    {
        EntityListHierarchy hierarchy = new EntityListHierarchyImpl(this, name);
        addHierarchy(hierarchy);
        return hierarchy;
    }

    /**
     * Creates a new EntitySetHierarchy with the specified name and adds it to this catalog.
     *
     * @param name the name of the hierarchy to create
     * @return the newly created EntitySetHierarchy
     */
    @Override
    public EntitySetHierarchy createEntitySet(@NotNull String name, long version, int initialCapacity)
    {
        EntitySetHierarchy hierarchy = new EntitySetHierarchyImpl(this, name);
        addHierarchy(hierarchy);
        return hierarchy;
    }

    /**
     * Creates a new EntityDirectoryHierarchy with the specified name and adds it to this catalog.
     *
     * @param name the name of the hierarchy to create
     * @return the newly created EntityDirectoryHierarchy
     */
    @Override
    public EntityDirectoryHierarchy createEntityDirectory(@NotNull String name, long version, int initialCapacity)
    {
        EntityDirectoryHierarchy hierarchy = new EntityDirectoryHierarchyImpl(this, name, version, initialCapacity);
        addHierarchy(hierarchy);
        return hierarchy;
    }

    /**
     * Creates a new EntityTreeHierarchy with the specified name and adds it to this catalog.
     *
     * @param name the name of the hierarchy to create
     * @return the newly created EntityTreeHierarchy
     */
    @Override
    public EntityTreeHierarchy createEntityTree(@NotNull String name, EntityTreeHierarchy.Node root, long version)
    {
        if (root == null) {
            root = new EntityTreeHierarchyImpl.NodeImpl(null);
        }
        EntityTreeHierarchy hierarchy = new EntityTreeHierarchyImpl(this, name, root, version);
        addHierarchy(hierarchy);
        return hierarchy;
    }

    /**
     * Creates a new AspectMapHierarchy for the specified AspectDef and adds it to this catalog.
     *
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @return the newly created AspectMapHierarchy
     */
    @Override
    public AspectMapHierarchy createAspectMap(@NotNull AspectDef aspectDef, long version)
    {
        AspectMapHierarchy hierarchy = new AspectMapHierarchyImpl(this, aspectDef);
        addHierarchy(hierarchy);
        return hierarchy;
    }
}
