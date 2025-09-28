package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all hierarchy types in the Cheap data model. A Hierarchy
 * represents the "H" in the Cheap acronym (Catalog, Hierarchy, Entity, Aspect, Property)
 * and provides organized access to entities within a catalog.
 * 
 * <p>Hierarchies define the structure and organization of entities, serving roles
 * analogous to tables or indexes in database terminology, or directory structures
 * in file systems. Each hierarchy has a specific type that determines its
 * organizational behavior and access patterns.</p>
 * 
 * <p>All hierarchies are associated with a catalog and have a definition that
 * specifies their type, name, and organizational characteristics.</p>
 */
public interface Hierarchy
{
    /**
     * Returns the definition that describes this hierarchy's characteristics,
     * including its type, name, and organizational parameters.
     *
     * <p>The hierarchy definition provides metadata about how this hierarchy
     * organizes entities and what operations are supported.</p>
     *
     * @return the hierarchy definition for this hierarchy, never null
     */
    @NotNull HierarchyDef def();

    /**
     * Returns the Catalog that contains this Hierarchy.
     *
     * @return the catalog that owns this hierarchy, never null
     */
    @NotNull Catalog catalog();

    /**
     * Returns the version number of this hierarchy.
     * Version numbers allow tracking changes and evolution of hierarchy contents over time.
     *
     * @return the version number of this hierarchy, defaults to 0
     */
    default long version()
    {
        return 0L;
    }
}
