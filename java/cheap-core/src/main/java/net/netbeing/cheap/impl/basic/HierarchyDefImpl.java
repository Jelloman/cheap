package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;

import java.util.Objects;

/**
 * Record-based implementation of HierarchyDef that defines the structure and
 * properties of a hierarchy in the Cheap system.
 *
 * @param name the name of the hierarchy
 * @param type the type of hierarchy
 *
 * @see HierarchyDef
 * @see HierarchyType
 */
public record HierarchyDefImpl(
        String name,
        HierarchyType type) implements HierarchyDef
{
    /**
     * Compact constructor that validates the hierarchy definition parameters.
     * 
     * @throws NullPointerException if name or type is null
     */
    public HierarchyDefImpl
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
    }
}
