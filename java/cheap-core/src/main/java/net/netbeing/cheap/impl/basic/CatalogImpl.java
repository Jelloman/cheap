package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

/**
 * Basic implementation of a Catalog in the CHEAP data caching system.
 * A catalog represents a complete data source or cache containing hierarchies
 * of entities and their aspects.
 * <p>
 * Catalogs can be either ROOT catalogs (representing external data sources)
 * or MIRROR catalogs (cached views of other catalogs). This implementation
 * maintains a directory of hierarchies and aspect definitions.
 * 
 * @see Catalog
 * @see CatalogDef
 * @see EntityFullImpl
 * @see HierarchyDir
 */
public class CatalogImpl extends EntityFullImpl implements Catalog
{
    /** The catalog definition describing this catalog's properties. */
    private final CatalogDef def;
    
    /** The upstream catalog this catalog mirrors, or null for root catalogs. */
    private final Catalog upstream;
    
    /** Directory of hierarchies contained in this catalog. */
    private final HierarchyDirImpl hierarchies;
    
    /** Directory of aspect definitions available in this catalog. */
    private final AspectDefDirHierarchyImpl aspectage;

    /**
     * Creates a new root catalog with default configuration.
     */
    public CatalogImpl()
    {
        this(new CatalogDefImpl(CatalogType.ROOT), null);
    }

    /**
     * Creates a new catalog with the specified definition.
     * For ROOT catalogs, upstream will be null. For MIRROR catalogs,
     * use the constructor that accepts an upstream catalog.
     * 
     * @param def the catalog definition describing this catalog
     */
    public CatalogImpl(CatalogDef def)
    {
        this(def, null);
    }

    /**
     * Creates a new catalog with the specified definition and upstream catalog.
     * 
     * @param def the catalog definition describing this catalog
     * @param upstream the upstream catalog to mirror, or null for root catalogs
     * @throws IllegalStateException if a root catalog has an upstream or a mirror catalog lacks one
     */
    public CatalogImpl(CatalogDef def, Catalog upstream)
    {
        super(def.globalId());
        this.def = def;
        this.upstream = upstream;

        switch (def.type()) {
            case ROOT -> {
                if (upstream != null) {
                    throw new IllegalStateException("Root catalogs may not have an upstream catalog.");
                }
            }
            case MIRROR -> {
                if (upstream == null) {
                    throw new IllegalStateException("Mirror catalogs must have an upstream catalog.");
                }
            }
        }

        this.hierarchies = new HierarchyDirImpl(CatalogDefaultHierarchies.CATALOG_ROOT);
        this.aspectage = new AspectDefDirHierarchyImpl(CatalogDefaultHierarchies.ASPECTAGE);
        hierarchies.put(CatalogDefaultHierarchies.CATALOG_ROOT_NAME, hierarchies);
        hierarchies.put(CatalogDefaultHierarchies.ASPECTAGE_NAME, aspectage);
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
     * Returns the upstream catalog this catalog mirrors.
     * 
     * @return the upstream catalog, or {@code null} for root catalogs
     */
    @Override
    public Catalog upstream()
    {
        return upstream;
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
     * Retrieves a hierarchy by name from this catalog.
     * 
     * @param name the name of the hierarchy to retrieve
     * @return the hierarchy with the given name, or {@code null} if not found
     */
    @Override
    public Hierarchy hierarchy(String name)
    {
        return hierarchies.get(name);
    }

    /**
     * Retrieves an aspect definition by name from this catalog's aspectage.
     * 
     * @param name the name of the aspect definition to retrieve
     * @return the aspect definition with the given name, or {@code null} if not found
     */
    @Override
    public AspectDef aspectDef(String name)
    {
        // TODO: aspectage
        return aspectage.get(name);
    }

}
