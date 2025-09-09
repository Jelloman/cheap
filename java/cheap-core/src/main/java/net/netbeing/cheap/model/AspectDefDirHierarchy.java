package net.netbeing.cheap.model;

/**
 * The interface Aspect def dir hierarchy.
 */
public interface AspectDefDirHierarchy extends Hierarchy
{
    /**
     * Add aspect def.
     *
     * @param def the def
     * @return the aspect def
     */
    AspectDef add(AspectDef def);

    /**
     * Get aspect def.
     *
     * @param name the name
     * @return the aspect def
     */
    AspectDef get(String name);
}
