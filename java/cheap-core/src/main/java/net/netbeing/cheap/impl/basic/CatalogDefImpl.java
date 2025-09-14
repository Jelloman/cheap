package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;

/**
 * Basic implementation of a CatalogDef that defines the structure and properties
 * of a catalog in the CHEAP data caching system.
 * <p>
 * This implementation automatically includes the default catalog hierarchies (catalog root and aspectage).
 * 
 * @see CatalogDef
 * @see CatalogSpecies
 * @see HierarchyDef
 */
public class CatalogDefImpl implements CatalogDef
{
    /** Map of hierarchy names to their definitions. */
    private final Map<String,HierarchyDef> hierarchyDefs = new LinkedHashMap<>(4);

    /** Map of hierarchy names to their definitions. */
    private final AspectDefDirImpl aspectDefs = new AspectDefDirImpl();

    /**
     * Creates a new CatalogDefImpl with no HierarchyDefs or AspectDefs.
     */
    public CatalogDefImpl()
    {
        this.hierarchyDefs.put(CatalogDefaultHierarchies.CATALOG_ROOT_NAME, CatalogDefaultHierarchies.CATALOG_ROOT);
        this.hierarchyDefs.put(CatalogDefaultHierarchies.ASPECTAGE_NAME, CatalogDefaultHierarchies.ASPECTAGE);
    }

    /**
     * Creates a new CatalogDefImpl as a copy of another CatalogDef.
     * 
     * @param other a CatalogDef
     */
    public CatalogDefImpl(@NotNull CatalogDef other)
    {
        this(other.hierarchyDefs(), other.aspectDefs());
    }

    /**
     * Creates a new CatalogDefImpl with copies of the provided hierarchy defs and/or aspect defs.
     * If the default hierarchies are not included, they will also be added.
     *
     * @param hierarchyDefs the hierarchyDefs to copy
     * @param aspectDefs the aspectDefs to copy
     */
    public CatalogDefImpl(Iterable<HierarchyDef> hierarchyDefs, Iterable<AspectDef> aspectDefs)
    {
        if (hierarchyDefs != null) {
            for (HierarchyDef hDef : hierarchyDefs) {
                this.hierarchyDefs.put(hDef.name(), hDef);
            }
        }
        if (aspectDefs != null) {
            for (AspectDef aDef : aspectDefs) {
                this.aspectDefs.add(aDef);
            }
        }
        if (!this.hierarchyDefs.containsKey(CatalogDefaultHierarchies.CATALOG_ROOT_NAME)) {
            this.hierarchyDefs.put(CatalogDefaultHierarchies.CATALOG_ROOT_NAME, CatalogDefaultHierarchies.CATALOG_ROOT);
        }
        if (!this.hierarchyDefs.containsKey(CatalogDefaultHierarchies.ASPECTAGE_NAME)) {
            this.hierarchyDefs.put(CatalogDefaultHierarchies.ASPECTAGE_NAME, CatalogDefaultHierarchies.ASPECTAGE);
        }
    }

    @Override
    public @NotNull AspectDefDir aspectDefs()
    {
        return aspectDefs;
    }

    /**
     * Returns an unmodifiable collection of all hierarchy defs in this catalog def.
     * 
     * @return collection of hierarchy definitions
     */
    @Override
    public @NotNull Collection<HierarchyDef> hierarchyDefs()
    {
        return Collections.unmodifiableCollection(hierarchyDefs.values());
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
