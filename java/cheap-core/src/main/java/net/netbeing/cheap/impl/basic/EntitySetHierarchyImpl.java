package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntitySetHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;

/**
 * Basic implementation of an EntitySetHierarchy using a HashSet.
 * This hierarchy type represents a non-ordered collection of unique entities
 * corresponding to the ENTITY_SET (ES) hierarchy type in Cheap.
 * <p>
 * This implementation extends HashSet to provide efficient entity membership
 * testing and duplicate prevention while implementing the EntitySetHierarchy interface.
 * 
 * @see EntitySetHierarchy
 * @see Entity
 * @see HierarchyDef
 */
public class EntitySetHierarchyImpl extends LinkedHashSet<Entity> implements EntitySetHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The hierarchy definition describing this entity set. */
    private final HierarchyDef def;

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition.
     * 
     * @param def the hierarchy definition for this entity set
     */
    public EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def)
    {
        this.catalog = catalog;
        this.def = def;
        catalog.addHierarchy(this);
    }

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition and
     * initial capacity.
     *
     * @param def the hierarchy definition for this entity set
     * @param initialCapacity initial capacity of set
     */
    public EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, int initialCapacity)
    {
        super(initialCapacity);
        this.catalog = catalog;
        this.def = def;
        catalog.addHierarchy(this);
    }

    /**
     * Returns the Catalog that owns this hierarchy.
     *
     * @return the parent catalog
     */
    @Override
    public @NotNull Catalog catalog()
    {
        return catalog;
    }

    /**
     * Returns the hierarchy definition for this entity set.
     * 
     * @return the hierarchy definition describing this entity set's structure
     */
    @Override
    public @NotNull HierarchyDef def()
    {
        return def;
    }
}
