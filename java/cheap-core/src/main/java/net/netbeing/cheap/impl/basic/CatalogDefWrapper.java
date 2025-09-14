package net.netbeing.cheap.impl.basic;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private final Map<String,HierarchyDef> hierarchyDefs;

    /**
     * Creates a new CatalogDefImpl with no HierarchyDefs or AspectDefs.
     */
    public CatalogDefWrapper(@NotNull Catalog catalog)
    {
        this.catalog = catalog;
        // Create a lazy wrapper around the catalog hierarchies
        this.hierarchyDefs = Maps.transformValues(catalog.hierarchies(), (Hierarchy h) -> h.def());
    //Collections2.transform(catalog.hierarchies().values(), (Hierarchy h) -> h.def());
    }

    /**
     * Returns the AspectDefDir of the wrapped Catalog.
     *
     * @return an AspectDefDir
     */
    @Override
    public @NotNull AspectDefDir aspectDefs()
    {
        return catalog.aspectDefs();
    }

    /**
     * Returns the hierarchy definitions in the wrapped catalog.
     * 
     * @return collection of hierarchy definitions
     */
    @Override
    public @NotNull Collection<HierarchyDef> hierarchyDefs()
    {
        return hierarchyDefs.values();
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
        return hierarchyDefs.get(name);
    }
}
