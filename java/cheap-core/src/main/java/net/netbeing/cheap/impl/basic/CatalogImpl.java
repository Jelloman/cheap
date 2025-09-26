package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
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
    /** The catalog definition describing this catalog's properties. */
    private final CatalogDef def;

    /** The immutable species of this Catalog. */
    private final CatalogSpecies species;

    /** The mutable uri of this Catalog. */
    private URI uri;

    /** Whether this catalog is strict. */
    private final boolean strict;

    /** The upstream catalog this catalog mirrors, or null for root catalogs. */
    private final UUID upstream;
    
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
        this(UUID.randomUUID(), CatalogSpecies.SINK, null, null, false);
    }

    /**
     * Creates a new non-strict SINK catalog with a wrapper CatalogDef that
     * fully delegates to this catalog.
     */
    public CatalogImpl(UUID globalId)
    {
        this(globalId, CatalogSpecies.SINK, null, null, false);
    }

    /**
     * Creates a new non-strict catalog with the specified species and upstream,
     * and a wrapper CatalogDef that fully delegates to this catalog.
     *
     * @throws IllegalArgumentException if a SOURCE/SINK catalog has an upstream; or for other species, if it lacks one
     */
    public CatalogImpl(CatalogSpecies species, UUID upstream)
    {
        this(UUID.randomUUID(), species, null, upstream, false);
    }

    /**
     * Creates a new catalog with the specified definition and upstream catalog.
     *
     * @param def the catalog definition describing this catalog
     * @param upstream the upstream catalog to mirror, or null for root catalogs
     * @throws IllegalArgumentException if a SOURCE/SINK catalog has an upstream; or for other species, if it lacks one
     */
    public CatalogImpl(UUID globalId, CatalogSpecies species, CatalogDef def, UUID upstream, boolean strict)
    {
        super(globalId, null);
        this.catalog = this;

        if (strict && def == null) {
            throw new IllegalArgumentException("A strict catalog may not be constructed without a CatalogDef.");
        }

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
        this.strict = strict;

        this.hierarchies = new LinkedHashMap<>();
        this.aspectage = new LinkedHashMap<>();

        this.def = def != null ? def : new CatalogDefWrapper(this);
    }

    /**
     * Returns the catalog definition for this catalog.
     * 
     * @return the catalog definition describing this catalog's properties
     */
    @Override
    public @NotNull CatalogDef def()
    {
        return def;
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
     * Flags whether this catalog is strict, which means it only contains the
     * AspectDefs and Hierarchies list in its CatalogDef. Non-strict catalogs
     * may contain additional Hierarchies and types of Aspects.
     *
     * @return whether this catalog is strict
     */
    @Override
    public boolean isStrict()
    {
        return strict;
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
        String hName = hierarchy.def().name();
        HierarchyDef existingDef = def.hierarchyDef(hName);
        if (strict && existingDef  == null) {
            throw new UnsupportedOperationException("A hierarchy may not be added to a strict Catalog unless it is part of the CatalogDef.");
        }
        if (existingDef != null && !existingDef.fullyEquals(hierarchy.def())) {
            throw new UnsupportedOperationException("A hierarchy may not be added to a strict Catalog unless it is part of the CatalogDef.");
        }
        Hierarchy existing = hierarchy(hName);
        if (existing instanceof AspectMapHierarchy) {
            throw new UnsupportedOperationException("A hierarchy may not be added to a Catalog with the same name as an existing AspectMapHierarchy.");
        }
        if (hierarchy instanceof AspectMapHierarchy) {
            AspectDef aspectDef = ((AspectMapHierarchy) hierarchy).aspectDef();
            aspectage.put(aspectDef.name(), aspectDef);
        }
        return hierarchies.put(hName, hierarchy);
    }

    @Override
    public boolean containsAspects(@NotNull String name)
    {
        if (hierarchies.get(name) instanceof AspectMapHierarchy) {
            return true; //!aMap.isEmpty();
        }
        return false;
    }
}
