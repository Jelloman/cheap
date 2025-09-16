package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.UUID;

/**
 * Full implementation of a Catalog in the CHEAP architecture. A catalog represents
 * either an external data source or a mirror/clone/fork of another catalog. A catalog
 * contains hierarchies of entities and their aspects.
 *
 * @see Catalog
 * @see CatalogDef
 * @see LocalEntity
 * @see LocalEntityOneCatalogImpl
 * @see HierarchyDir
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
    private final Catalog upstream;
    
    /** Directory of hierarchies contained in this catalog. */
    private final HierarchyDirImpl hierarchies;
    
    /** Directory of aspect definitions available in this catalog. */
    private final AspectDefDirHierarchyImpl aspectage;

    /**
     * Creates a new non-strict SINK catalog with a wrapper CatalogDef that
     * fully delegates to this catalog.
     */
    public CatalogImpl()
    {
        this(UUID.randomUUID(), CatalogSpecies.SINK, null, null, false);
    }

    /**
     * Creates a new non-strict catalog with the specified species and upstream,
     * and a wrapper CatalogDef that fully delegates to this catalog.
     *
     * @throws IllegalArgumentException if a SOURCE/SINK catalog has an upstream; or for other species, if it lacks one
     */
    public CatalogImpl(CatalogSpecies species, Catalog upstream)
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
    public CatalogImpl(UUID globalId, CatalogSpecies species, CatalogDef def, Catalog upstream, boolean strict)
    {
        //noinspection DataFlowIssue
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

        this.hierarchies = new HierarchyDirImpl(CatalogDefaultHierarchies.CATALOG_ROOT);
        this.aspectage = new AspectDefDirHierarchyImpl(CatalogDefaultHierarchies.ASPECTAGE);
        hierarchies.put(CatalogDefaultHierarchies.CATALOG_ROOT_NAME, hierarchies);
        hierarchies.put(CatalogDefaultHierarchies.ASPECTAGE_NAME, aspectage);

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
    public Catalog upstream()
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
     * Returns the directory of hierarchies in this catalog.
     * 
     * @return the hierarchy directory containing all hierarchies in this catalog
     */
    @Override
    public @NotNull HierarchyDir hierarchies()
    {
        return hierarchies;
    }

    /**
     * Returns the directory of all AspectDefs contained within this catalog.
     * This is always a superset of the AspectDefDir provided by the CatalogDef.
     *
     * <p>This is one of the two fixed hierarchies present in every catalog.</p>
     *
     * @return the AspectDef directory for this catalog, never null
     */
    @Override
    public @NotNull AspectDefDir aspectDefs()
    {
        return aspectage;
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

}
