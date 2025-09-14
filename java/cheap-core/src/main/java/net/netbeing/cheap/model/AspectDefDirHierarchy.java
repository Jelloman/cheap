package net.netbeing.cheap.model;

/**
 * A directory hierarchy that manages aspect definitions by name.
 * This hierarchy type provides a mapping from string names to AspectDef instances,
 * allowing for organized storage and retrieval of aspect definitions within a catalog.
 * 
 * <p>This corresponds to the ASPECT_DEF_DIR (ED) hierarchy type in the CHEAP model,
 * specifically designed for managing aspect definitions. It enables catalogs to
 * maintain a registry of available aspect types that can be used to create
 * aspect instances.</p>
 */
public interface AspectDefDirHierarchy extends Hierarchy, AspectDefDir
{
    /**
     * Adds an aspect definition to this directory hierarchy.
     * The definition will be stored under its name for later retrieval.
     * 
     * @param def the aspect definition to add
     * @return the previous aspect definition with the same name, or null if none existed
     */
    AspectDef add(AspectDef def);
}
