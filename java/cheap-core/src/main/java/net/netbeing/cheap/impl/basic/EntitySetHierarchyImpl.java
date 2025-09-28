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

    /** The version number of this hierarchy. */
    private final long version;

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition.
     *
     * @param def the hierarchy definition for this entity set
     */
    public EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def)
    {
        this(catalog, def, 0L);
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
        this(catalog, def, initialCapacity, 0L);
    }

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param def the hierarchy definition for this entity set
     * @param version the version number of this hierarchy
     */
    public EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, long version)
    {
        this.catalog = catalog;
        this.def = def;
        this.version = version;
        catalog.addHierarchy(this);
    }

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition,
     * initial capacity, and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param def the hierarchy definition for this entity set
     * @param initialCapacity initial capacity of set
     * @param version the version number of this hierarchy
     */
    public EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, int initialCapacity, long version)
    {
        super(initialCapacity);
        this.catalog = catalog;
        this.def = def;
        this.version = version;
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

    /**
     * Returns the version number of this hierarchy.
     *
     * @return the version number
     */
    @Override
    public long version()
    {
        return version;
    }
}
