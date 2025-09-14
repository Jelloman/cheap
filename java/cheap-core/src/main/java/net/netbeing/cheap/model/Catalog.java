package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.UUID;

/**
 * Represents a catalog in the CHEAP data model, serving as the "C" in the CHEAP acronym
 * (Catalog, Hierarchy, Entity, Aspect, Property). A Catalog is the top-level container
 * that organizes and provides access to hierarchies of entities and their aspects.
 * 
 * <p>Catalogs are analogous to databases in traditional database systems or volumes
 * in file systems. They serve as the primary caching layer and provide the organizational
 * structure for all data within their scope.</p>
 * 
 * <p>All catalogs in CHEAP are caches by design, with different types (ROOT, MIRROR)
 * determining their relationship to upstream data sources. Catalogs maintain two
 * special hierarchies: one for managing other hierarchies and one for managing
 * aspect definitions.</p>
 * 
 * <p>Catalogs extend Entity, meaning they have their own global identity and can
 * have aspects attached to them, allowing for metadata about the catalog itself.</p>
 */
public interface Catalog extends Entity
{
    /**
     * Returns the catalog definition that describes this catalog's characteristics,
     * including its type, caching behavior, and relationship to upstream sources.
     * 
     * <p>The catalog definition provides metadata about how this catalog operates
     * and interacts with other catalogs in the system.</p>
     *
     * @return the catalog definition for this catalog, never null
     */
    @NotNull CatalogDef def();

    /**
     * Returns the species of this catalog.
     *
     * @return the catalog species
     */
    @NotNull CatalogSpecies species();

    /**
     * Returns the globally unique identifier for this catalog.
     *
     * @return the UUID identifying this catalog globally
     */
    @NotNull UUID globalId();

    /**
     * The URI of this catalog. Usually a URL, but need not be.
     * Cheap is not concerned with network layers, only modeling.
     *
     * @return the location of this catalog
     */
    @NotNull URI uri();

    /**
     * Returns the upstream catalog that this catalog derives its data from,
     * or null if this is a source or sink catalog without an upstream source.
     * 
     * @return the upstream catalog, or null if this is a source or sink
     */
    Catalog upstream();

    /**
     * Flags whether this catalog is strict, which means it only contains the
     * AspectDefs and Hierarchies list in its CatalogDef. Non-strict catalogs
     * may contain additional Hierarchies and types of Aspects.
     *
     * @return whether this catalog is strict
     */
    boolean isStrict();

    /**
     * Returns the directory of hierarchies contained within this catalog.
     * This special hierarchy manages the collection of all other hierarchies
     * in the catalog and provides named access to them.
     *
     * <p>This is one of the two fixed hierarchies present in every catalog.</p>
     *
     * @return the hierarchy directory for this catalog, never null
     */
    @NotNull HierarchyDir hierarchies();

    /**
     * Returns the directory of all AspectDefs contained within this catalog.
     * This is always a superset of the AspectDefDir provided by the CatalogDef.
     *
     * <p>This is one of the two fixed hierarchies present in every catalog.</p>
     *
     * @return the AspectDef directory for this catalog, never null
     */
    @NotNull AspectDefDir aspectDefs();

    /**
     * Retrieves a specific hierarchy by name from this catalog.
     * 
     * <p>This is a convenience method that provides direct access to named
     * hierarchies without requiring navigation through the hierarchies directory.</p>
     *
     * @param name the name of the hierarchy to retrieve, may be null
     * @return the hierarchy with the specified name, or null if not found
     */
    Hierarchy hierarchy(@NotNull String name);

    /**
     * Retrieves the aspect map hierarchy for a specific aspect definition.
     * This provides access to all aspects of the specified type.
     * 
     * @param aspectDef the aspect definition to find aspects for, must not be null
     * @return the aspect map hierarchy containing all aspects of the specified type
     */
    default AspectMapHierarchy aspects(@NotNull AspectDef aspectDef)
    {
        return (AspectMapHierarchy) hierarchy(aspectDef.name());
    }

    /**
     * Retrieves the aspect map hierarchy for a specific aspect definition by name.
     *
     * <p>This method combines aspect definition lookup by name with hierarchy
     * retrieval to provide a single-step access to aspect collections by name.</p>
     *
     * @param name the name of the aspect definition to find aspects for, may not be null
     * @return the aspect map hierarchy containing all aspects of the specified type,
     *         or null if the aspect definition is not found
     */
    default AspectMapHierarchy aspects(@NotNull String name)
    {
        AspectDef aspectDef = aspectDefs().get(name);
        return aspectDef == null ? null : aspects(aspectDef);
    }


}
