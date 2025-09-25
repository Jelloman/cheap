package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityDirectoryHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

/**
 * Basic implementation of an EntityDirectoryHierarchy using a HashMap.
 * This hierarchy type represents a string-to-entity mapping, corresponding
 * to the ENTITY_DIR (ED) hierarchy type in Cheap.
 * <p>
 * This implementation extends HashMap to provide efficient name-based entity
 * lookup while implementing the EntityDirectoryHierarchy interface.
 * 
 * @see EntityDirectoryHierarchy
 * @see Entity
 * @see HierarchyDef
 */
public class EntityDirectoryHierarchyImpl extends LinkedHashMap<String, Entity> implements EntityDirectoryHierarchy
{
    /** The hierarchy definition describing this entity directory. */
    private final HierarchyDef def;

    /**
     * Creates a new EntityDirectoryHierarchyImpl with the specified hierarchy definition.
     * 
     * @param def the hierarchy definition for this entity directory
     */
    public EntityDirectoryHierarchyImpl(HierarchyDef def)
    {
        this.def = def;
    }

    /**
     * Returns the hierarchy definition for this entity directory.
     * 
     * @return the hierarchy definition describing this entity directory's structure
     */
    @Override
    public @NotNull HierarchyDef def()
    {
        return def;
    }
}
