package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

/**
 * A CatalogDef defines the structure and properties of a catalog.
 *
 * @see AspectDef
 * @see HierarchyDef
 */
public interface CatalogDef
{
    /**
     * Returns the globally unique identifier for this CatalogDef.
     *
     * @return the UUID identifying this CatalogDef globally
     */
    @NotNull UUID globalId();

    /**
     * Returns a read-only collection of the aspect definitions that are typically found
     * in a catalog with this definition. Catalogs flagged as "strict" will only contain
     * these Aspects; otherwise they may contain additional types of Aspects.
     *
     * @return collection of aspect definitions
     */
    @NotNull Iterable<AspectDef> aspectDefs();

    /**
     * Returns  a read-only collection of hierarchy definitions that are typically found
     * in a catalog with this definition. Catalogs flagged as "strict" will only contain
     * these Hierarchies; otherwise they may contain additional Hierarchies.
     *
     * @return collection of hierarchy definitions
     */
    @NotNull Iterable<HierarchyDef> hierarchyDefs();

    /**
     * Retrieves a hierarchy definition by name.
     *
     * @param name the name of the hierarchy definition to retrieve
     * @return the hierarchy definition with the given name, or {@code null} if not found
     */
    HierarchyDef hierarchyDef(String name);

    /**
     * Retrieves an aspect definition by name.
     *
     * @param name the name of the aspect definition to retrieve
     * @return the aspect definition with the given name, or {@code null} if not found
     */
    AspectDef aspectDef(String name);

}
