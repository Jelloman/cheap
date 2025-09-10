package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;

import java.util.Objects;

/**
 * Record-based implementation of HierarchyDef that defines the structure and
 * properties of a hierarchy in the CHEAP system.
 * <p>
 * This implementation uses Java records to provide an immutable hierarchy
 * definition with automatic equals, hashCode, and toString implementations.
 * Hierarchy definitions specify the hierarchy type and modification permissions.
 * 
 * @param name the name of the hierarchy
 * @param type the type of hierarchy (ENTITY_LIST, ENTITY_SET, ENTITY_DIR, ENTITY_TREE, or ASPECT_SET)
 * @param isModifiable whether the hierarchy contents can be modified
 * @param isImmutable whether the hierarchy definition itself is immutable
 * 
 * @see HierarchyDef
 * @see HierarchyType
 */
public record HierarchyDefImpl(
        String name,
        HierarchyType type,
        boolean isModifiable,
        boolean isImmutable) implements HierarchyDef
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

    /**
     * Creates a HierarchyDefImpl with default settings (modifiable and immutable).
     * 
     * @param name the name of the hierarchy
     * @param type the type of hierarchy
     */
    public HierarchyDefImpl(String name, HierarchyType type)
    {
        this(name, type, true, true);
    }
}
