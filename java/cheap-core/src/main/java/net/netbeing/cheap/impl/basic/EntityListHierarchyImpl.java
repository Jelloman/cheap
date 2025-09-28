package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityListHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Basic implementation of an EntityListHierarchy using an ArrayList.
 * This hierarchy type represents an ordered list of entities that may contain
 * duplicates, corresponding to the ENTITY_LIST (EL) hierarchy type in Cheap.
 * <p>
 * This implementation extends ArrayList to provide indexed access and maintain
 * insertion order while implementing the EntityListHierarchy interface.
 * 
 * @see EntityListHierarchy
 * @see Entity
 * @see HierarchyDef
 */
public class EntityListHierarchyImpl extends ArrayList<Entity> implements EntityListHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The hierarchy definition describing this entity list. */
    private final HierarchyDef def;

    /** The version number of this hierarchy. */
    private final long version;

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition.
     *
     * @param def the hierarchy definition for this entity list
     */
    public EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def)
    {
        this(catalog, def, 0L);
    }

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition and
     * initial capacity.
     *
     * @param def the hierarchy definition for this entity list
     * @param initialCapacity initial capacity of list
     */
    public EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, int initialCapacity)
    {
        this(catalog, def, initialCapacity, 0L);
    }

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param def the hierarchy definition for this entity list
     * @param version the version number of this hierarchy
     */
    public EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, long version)
    {
        this.catalog = catalog;
        this.def = def;
        this.version = version;
        catalog.addHierarchy(this);
    }

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition,
     * initial capacity, and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param def the hierarchy definition for this entity list
     * @param initialCapacity initial capacity of list
     * @param version the version number of this hierarchy
     */
    public EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, int initialCapacity, long version)
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
     * Returns the hierarchy definition for this entity list.
     *
     * @return the hierarchy definition describing this entity list's structure
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
