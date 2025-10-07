package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
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

    /** The name of this hierarchy in the catalog. */
    private final String name;

    /** The version number of this hierarchy. */
    private final long version;

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     */
    public EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull String name)
    {
        this(catalog, name, 0L);
    }

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition and
     * initial capacity.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param initialCapacity initial capacity of set
     */
    public EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, int initialCapacity)
    {
        this(catalog, name, initialCapacity, 0L);
    }

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param version the version number of this hierarchy
     */
    public EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, long version)
    {
        this.catalog = catalog;
        this.name = name;
        this.version = version;
        catalog.addHierarchy(this);
    }

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition,
     * initial capacity, and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param initialCapacity initial capacity of set
     * @param version the version number of this hierarchy
     */
    public EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, int initialCapacity, long version)
    {
        super(initialCapacity);
        this.catalog = catalog;
        this.name = name;
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
     * Returns the name of this hierarchy in the catalog.
     *
     * @return the name of the hierarchy
     */
    @Override
    public @NotNull String name()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull HierarchyType type()
    {
        return HierarchyType.ENTITY_SET;
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
