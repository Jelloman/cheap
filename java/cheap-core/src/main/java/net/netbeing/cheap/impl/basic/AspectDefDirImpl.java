package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
public class AspectDefDirImpl implements AspectDefDir
{
    /** Internal map storing aspect definition names to aspect definitions. */
    private final LinkedHashMap<String, AspectDef> aspectDefs = new LinkedHashMap<>();

    public AspectDefDirImpl()
    {
    }

    /**
     * Adds an aspect definition to this hierarchy directory.
     * The definition is stored using its name as the key.
     * 
     * @param def the aspect definition to add
     * @return the previous aspect definition with the same name, or {@code null} if none existed
     */
    public AspectDef add(AspectDef def)
    {
        return aspectDefs.put(def.name(), def);
    }

    /**
     * Returns a read-only collection of the AspectDefs in this directory.
     *
     * @return the AspectDefs in this directory.
     */
    @Override
    public Collection<AspectDef> aspectDefs()
    {
        return Collections.unmodifiableCollection(aspectDefs.values());
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
     * Provides an Iterator over the AspectDefss in this directory.
     *
     * @return an iterator of this directory's contents
     */
    @Override
    public @NotNull Iterator<AspectDef> iterator()
    {
        return aspectDefs().iterator();
    }
}
