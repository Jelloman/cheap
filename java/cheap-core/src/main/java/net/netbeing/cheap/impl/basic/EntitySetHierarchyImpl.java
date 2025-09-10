package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.EntitySetHierarchy;

import java.util.HashSet;

/**
 * Basic implementation of an EntitySetHierarchy using a HashSet.
 * This hierarchy type represents a non-ordered collection of unique entities
 * corresponding to the ENTITY_SET (ES) hierarchy type in CHEAP.
 * <p>
 * This implementation extends HashSet to provide efficient entity membership
 * testing and duplicate prevention while implementing the EntitySetHierarchy interface.
 * 
 * @see EntitySetHierarchy
 * @see Entity
 * @see HierarchyDef
 */
public class EntitySetHierarchyImpl extends HashSet<Entity> implements EntitySetHierarchy
{
    /** The hierarchy definition describing this entity set. */
    private final HierarchyDef def;

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition.
     * 
     * @param def the hierarchy definition for this entity set
     */
    public EntitySetHierarchyImpl(HierarchyDef def)
    {
        this.def = def;
    }

    /**
     * Returns the hierarchy definition for this entity set.
     * 
     * @return the hierarchy definition describing this entity set's structure
     */
    @Override
    public HierarchyDef def()
    {
        return def;
    }
}
