package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

/**
 * The interface Catalog.
 */
public interface Catalog extends Entity
{
    /**
     * Def catalog def.
     *
     * @return the catalog def
     */
    @NotNull CatalogDef def();

    /**
     * Upstream catalog.
     *
     * @return the catalog
     */
    Catalog upstream();

    /**
     * Hierarchies hierarchy dir.
     *
     * @return the hierarchy dir
     */
    @NotNull HierarchyDir hierarchies();

    /**
     * Hierarchy hierarchy.
     *
     * @param name the name
     * @return the hierarchy
     */
    Hierarchy hierarchy(String name);

    /**
     * Aspect def aspect def.
     *
     * @param name the name
     * @return the aspect def
     */
    AspectDef aspectDef(String name);

    /**
     * Aspects aspect map hierarchy.
     *
     * @param aspectDef the aspect def
     * @return the aspect map hierarchy
     */
    default AspectMapHierarchy aspects(AspectDef aspectDef)
    {
        return (AspectMapHierarchy) hierarchy(aspectDef.name());
    }

    /**
     * Aspects aspect map hierarchy.
     *
     * @param name the name
     * @return the aspect map hierarchy
     */
    default AspectMapHierarchy aspects(String name)
    {
        AspectDef aspectDef = aspectDef(name);
        return aspects(aspectDef);
    }


}
