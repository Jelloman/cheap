package net.netbeing.cheap.impl.basic;

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
    /** The hierarchy definition describing this entity list. */
    private final HierarchyDef def;

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition.
     *
     * @param def the hierarchy definition for this entity list
     */
    public EntityListHierarchyImpl(HierarchyDef def)
    {
        this.def = def;
    }

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition and
     * initial capacity.
     *
     * @param def the hierarchy definition for this entity list
     * @param initialCapacity initial capacity of list
     */
    public EntityListHierarchyImpl(HierarchyDef def, int initialCapacity)
    {
        super(initialCapacity);
        this.def = def;
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
}
