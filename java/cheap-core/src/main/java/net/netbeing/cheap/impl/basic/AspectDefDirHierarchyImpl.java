package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectDefDirHierarchy;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyDef;

import java.util.LinkedHashMap;

/**
 * Basic implementation of an AspectDefDirHierarchy that stores aspect definitions
 * in a directory-like structure using string names as keys.
 * <p>
 * This implementation uses a {@link LinkedHashMap} to maintain insertion order
 * of aspect definitions while providing efficient lookup by name.
 * 
 * @see AspectDefDirHierarchy
 * @see AspectDef
 * @see Hierarchy
 */
public class AspectDefDirHierarchyImpl implements AspectDefDirHierarchy
{
    /** Internal map storing aspect definition names to aspect definitions. */
    private final LinkedHashMap<String, AspectDef> aspectDefs = new LinkedHashMap<>();
    private final HierarchyDef def;

    public AspectDefDirHierarchyImpl(HierarchyDef def)
    {
        this.def = def;
    }

    /**
     * Adds an aspect definition to this hierarchy directory.
     * The definition is stored using its name as the key.
     * 
     * @param def the aspect definition to add
     * @return the previous aspect definition with the same name, or {@code null} if none existed
     */
    @Override
    public AspectDef add(AspectDef def)
    {
        return aspectDefs.put(def.name(), def);
    }

    /**
     * Retrieves an aspect definition by name from this hierarchy directory.
     * 
     * @param name the name of the aspect definition to retrieve
     * @return the aspect definition with the given name, or {@code null} if not found
     */
    @Override
    public AspectDef get(String name)
    {
        return aspectDefs.get(name);
    }

    /**
     * Returns the hierarchy definition for this hierarchy.
     *
     * @return the hierarchy definition for this hierarchy.
     */
    @Override
    public HierarchyDef def()
    {
        return def;
    }
}
