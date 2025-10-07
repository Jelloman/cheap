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
     * Returns the Catalog that contains this Hierarchy.
     *
     * @return the catalog that owns this hierarchy, never null
     */
    @NotNull Catalog catalog();

    /**
     * Returns the name identifier for this hierarchy definition, which is unique
     * within its catalog.
     *
     * @return the hierarchy name, never null
     */
    @NotNull String name();

    /**
     * Returns the type of hierarchy this definition describes.
     * The type determines the structure and behavior of hierarchy instances
     * created from this definition.
     *
     * @return the hierarchy type, never null
     */
    @NotNull HierarchyType type();

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
