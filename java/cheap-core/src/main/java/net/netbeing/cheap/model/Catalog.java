package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

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
     * Returns the upstream catalog that this catalog derives its data from,
     * or null if this is a root catalog without an upstream source.
     * 
     * <p>Mirror catalogs have upstream catalogs, while root catalogs represent
     * authoritative data sources and have no upstream.</p>
     *
     * @return the upstream catalog, or null if this is a root catalog
     */
    Catalog upstream();

    /**
     * Returns the directory of hierarchies contained within this catalog.
     * This special hierarchy manages the collection of all other hierarchies
     * in the catalog and provides named access to them.
     * 
     * <p>This is one of the two fixed hierarchies present in every catalog
     * (hierarchy 0 in the CHEAP model specification).</p>
     *
     * @return the hierarchy directory for this catalog, never null
     */
    @NotNull HierarchyDir hierarchies();

    /**
     * Retrieves a specific hierarchy by name from this catalog.
     * 
     * <p>This is a convenience method that provides direct access to named
     * hierarchies without requiring navigation through the hierarchies directory.</p>
     *
     * @param name the name of the hierarchy to retrieve, may be null
     * @return the hierarchy with the specified name, or null if not found
     */
    Hierarchy hierarchy(String name);

    /**
     * Retrieves a specific aspect definition by name from this catalog.
     * 
     * <p>Aspect definitions define the schema for aspects and are managed
     * through the catalog's aspect definition directory hierarchy.</p>
     *
     * @param name the name of the aspect definition to retrieve, may be null
     * @return the aspect definition with the specified name, or null if not found
     */
    AspectDef aspectDef(String name);

    /**
     * Retrieves the aspect map hierarchy for a specific aspect definition.
     * This provides access to all entities that have aspects of the specified type.
     * 
     * <p>This is a convenience method that combines aspect definition lookup
     * with hierarchy retrieval to provide direct access to aspect collections.</p>
     *
     * @param aspectDef the aspect definition to find aspects for, must not be null
     * @return the aspect map hierarchy containing all aspects of the specified type
     */
    default AspectMapHierarchy aspects(AspectDef aspectDef)
    {
        return (AspectMapHierarchy) hierarchy(aspectDef.name());
    }

    /**
     * Retrieves the aspect map hierarchy for a specific aspect definition by name.
     * This is a convenience method that looks up the aspect definition and then
     * retrieves its corresponding aspect map hierarchy.
     * 
     * <p>This method combines aspect definition lookup by name with hierarchy
     * retrieval to provide a single-step access to aspect collections.</p>
     *
     * @param name the name of the aspect definition to find aspects for, may be null
     * @return the aspect map hierarchy containing all aspects of the specified type,
     *         or null if the aspect definition is not found
     */
    default AspectMapHierarchy aspects(String name)
    {
        AspectDef aspectDef = aspectDef(name);
        return aspects(aspectDef);
    }


}
