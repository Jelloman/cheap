package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

public interface Catalog extends Entity
{
    @NotNull CatalogDef def();

    Catalog upstream();

    @NotNull HierarchyDir hierarchies();

    Hierarchy hierarchy(String name);

    AspectDef aspectDef(String name);

    default AspectMapHierarchy aspects(AspectDef aspectDef)
    {
        return (AspectMapHierarchy) hierarchy(aspectDef.name());
    }

    default AspectMapHierarchy aspects(String name)
    {
        AspectDef aspectDef = aspectDef(name);
        return aspects(aspectDef);
    }


}
