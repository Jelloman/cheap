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
    /** The hierarchy definition describing this hierarchy's structure. */
    private final HierarchyDef def;
    
    /** The aspect definition for the aspects stored in this hierarchy. */
    private final AspectDef aspectDef;

    /**
     * Creates a new AspectMapHierarchyImpl with the given definitions.
     * 
     * @param def the hierarchy definition for this hierarchy
     * @param aspectDef the aspect definition for aspects in this hierarchy
     */
    public AspectMapHierarchyImpl(HierarchyDef def, AspectDef aspectDef)
    {
        this.def = def;
        this.aspectDef = aspectDef;
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
