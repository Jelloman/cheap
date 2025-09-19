package net.netbeing.cheap.impl.basic;

import com.google.common.collect.Iterables;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

/**
 * Thin implementation of CatalogDef that merely wraps an existing Catalog.
 *
 * @see CatalogDef
 * @see CatalogSpecies
 * @see HierarchyDef
 */
public class CatalogDefWrapper implements CatalogDef
{
    private final Catalog catalog;

    /**
     * Creates a new CatalogDefImpl with no HierarchyDefs or AspectDefs.
     */
    public CatalogDefWrapper(@NotNull Catalog catalog)
    {
        this.catalog = catalog;
    }

    /**
     * Returns the AspectDefDir of the wrapped Catalog.
     *
     * @return an AspectDefDir
     */
    @Override
    public @NotNull Iterable<AspectDef> aspectDefs()
    {
        return catalog.aspectDefs();
    }

    /**
     * Returns the hierarchy definitions in the wrapped catalog.
     * 
     * @return collection of hierarchy definitions
     */
    @Override
    public @NotNull Iterable<HierarchyDef> hierarchyDefs()
    {
        return Iterables.transform(catalog.hierarchies(), h -> h.def());
    }

    /**
     * Retrieves a hierarchy definition by name.
     * 
     * @param name the name of the hierarchy definition to retrieve
     * @return the hierarchy definition with the given name, or {@code null} if not found
     */
    @Override
    public HierarchyDef hierarchyDef(String name)
    {
        Hierarchy hierarchy = catalog.hierarchy(name);
        return hierarchy != null ? hierarchy.def() : null;
    }

    /**
     * Retrieves an aspect definition by name.
     *
     * @param name the name of the aspect definition to retrieve
     * @return the aspect definition with the given name, or {@code null} if not found
     */
    @Override
    public AspectDef aspectDef(String name)
    {
        if (!catalog.containsAspects(name)) {
            return null;
        }
        AspectMapHierarchy aMap = catalog.aspects(name);
        return aMap != null ? aMap.aspectDef() : null;
    }
}
