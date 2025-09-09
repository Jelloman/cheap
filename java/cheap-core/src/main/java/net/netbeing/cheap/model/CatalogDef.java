package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * The interface Catalog def.
 */
public interface CatalogDef
{
    /**
     * Hierarchy defs collection.
     *
     * @return the collection
     */
    @NotNull Collection<HierarchyDef> hierarchyDefs();

    /**
     * Hierarchy def hierarchy def.
     *
     * @param name the name
     * @return the hierarchy def
     */
    HierarchyDef hierarchyDef(String name);

    /**
     * Global id uuid.
     *
     * @return the uuid
     */
    @NotNull UUID globalId();

    /**
     * Type catalog type.
     *
     * @return the catalog type
     */
    @NotNull CatalogType type();
}
