package net.netbeing.cheap.model;

import java.util.Collection;

/**
 * A directory that provides aspect definitions by name.
 */
public interface AspectDefDir extends Iterable<AspectDef>
{
    /**
     * Retrieves a read-only collection of AspectDefs.
     * 
     * @return a collection of AspectDefs
     */
    Collection<AspectDef> aspectDefs();

    /**
     * Retrieves an aspect definition by its name.
     *
     * @param name the name of the aspect definition to retrieve
     * @return the aspect definition with the specified name, or null if not found
     */
    AspectDef get(String name);
}
