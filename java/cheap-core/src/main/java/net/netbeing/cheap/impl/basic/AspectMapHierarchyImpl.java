package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Basic implementation of an AspectMapHierarchy that maps entities to aspects.
 * This hierarchy type stores a mapping from entity IDs to aspects of a single type.
 * <p>
 * This class extends {@link HashMap} to provide efficient entity-to-aspect lookups
 * while implementing the {@link AspectMapHierarchy} interface.
 * 
 * @see AspectMapHierarchy
 * @see Hierarchy
 */
public class AspectMapHierarchyImpl extends HashMap<Entity, Aspect> implements AspectMapHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The hierarchy definition describing this hierarchy's structure. */
    private final HierarchyDef def;

    /** The aspect definition for the aspects stored in this hierarchy. */
    private final AspectDef aspectDef;

    /**
     * Creates a new AspectMapHierarchyImpl to contain the given AspectDef.
     * AA new HierarchyDef will be constructed.
     *
     * @param aspectDef the aspect definition for aspects in this hierarchy
     */
    public AspectMapHierarchyImpl(@NotNull Catalog catalog, @NotNull AspectDef aspectDef)
    {
        this.catalog = catalog;
        this.aspectDef = aspectDef;
        this.def = new HierarchyDefImpl(aspectDef.name(), HierarchyType.ASPECT_MAP);
        catalog.addHierarchy(this);
    }

    /**
     * Creates a new AspectMapHierarchyImpl with the given definitions.
     *
     * @param def the hierarchy definition for this hierarchy
     * @param aspectDef the aspect definition for aspects in this hierarchy
     */
    public AspectMapHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, @NotNull AspectDef aspectDef)
    {
        this.catalog = catalog;
        this.def = def;
        this.aspectDef = aspectDef;
        catalog.addHierarchy(this);
    }

    /**
     * Returns the aspect definition for aspects stored in this hierarchy.
     * 
     * @return the aspect definition
     */
    @Override
    public AspectDef aspectDef()
    {
        return aspectDef;
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
     * Returns the hierarchy definition for this hierarchy.
     *
     * @return the hierarchy definition
     */
    @Override
    public @NotNull HierarchyDef def()
    {
        return def;
    }
}
