package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Basic implementation of a CatalogDef that defines the structure and properties
 * of a catalog in the CHEAP data caching system.
 * <p>
 * This implementation manages catalog type, global identifier, and hierarchy definitions.
 * It automatically includes the default catalog hierarchies (catalog root and aspectage).
 * 
 * @see CatalogDef
 * @see CatalogType
 * @see HierarchyDef
 */
public class CatalogDefImpl implements CatalogDef
{
    /** The type of this catalog (ROOT or MIRROR). */
    private final CatalogType type;
    
    /** The globally unique identifier for this catalog. */
    private final UUID globalId;
    
    /** Map of hierarchy names to their definitions. */
    private final Map<String,HierarchyDef> hierarchyDefs = new LinkedHashMap<>(4);

    /**
     * Creates a new CatalogDefImpl with MIRROR type and a random UUID.
     */
    public CatalogDefImpl()
    {
        this(CatalogType.MIRROR, UUID.randomUUID());
    }

    /**
     * Creates a new CatalogDefImpl with the specified type and a random UUID.
     * 
     * @param type the catalog type (ROOT or MIRROR)
     */
    public CatalogDefImpl(CatalogType type)
    {
        this(type, UUID.randomUUID());
    }

    /**
     * Creates a new CatalogDefImpl with the specified type and global ID.
     * Automatically includes default catalog hierarchies.
     * 
     * @param type the catalog type (ROOT or MIRROR)
     * @param globalId the globally unique identifier for this catalog
     */
    public CatalogDefImpl(CatalogType type, UUID globalId)
    {
        this.type = type;
        this.globalId = globalId;

        hierarchyDefs.put(CatalogDefaultHierarchies.CATALOG_ROOT_NAME, CatalogDefaultHierarchies.CATALOG_ROOT);
        hierarchyDefs.put(CatalogDefaultHierarchies.ASPECTAGE_NAME, CatalogDefaultHierarchies.ASPECTAGE);
    }

    /**
     * Returns the type of this catalog.
     * 
     * @return the catalog type (ROOT or MIRROR)
     */
    @Override
    public @NotNull CatalogType type()
    {
        return type;
    }

    /**
     * Returns the globally unique identifier for this catalog.
     * 
     * @return the UUID identifying this catalog globally
     */
    @Override
    public @NotNull UUID globalId()
    {
        return globalId;
    }

    /**
     * Returns an unmodifiable collection of all hierarchy definitions in this catalog.
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
