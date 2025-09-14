package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

/**
 * Basic implementation of an AspectDefDirHierarchy that stores aspect definitions
 * in a directory-like structure using string names as keys.
 * <p>
 * This implementation uses a {@link LinkedHashMap} to maintain insertion order
 * of aspect definitions while providing efficient lookup by name.
 * 
 * @see AspectDefDirHierarchy
 * @see AspectDefDir
 * @see AspectDef
 * @see Hierarchy
 */
public class AspectDefDirHierarchyImpl extends AspectDefDirImpl implements AspectDefDirHierarchy
{
    /** The def for this hierarchy. */
    private final HierarchyDef def;

    public AspectDefDirHierarchyImpl(HierarchyDef def)
    {
        this.def = def;
    }

    /**
     * Returns the hierarchy definition for this hierarchy.
     *
     * @return the hierarchy definition for this hierarchy.
     */
    @Override
    public @NotNull HierarchyDef def()
    {
        return def;
    }
}
