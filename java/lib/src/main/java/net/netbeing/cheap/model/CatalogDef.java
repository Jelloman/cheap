package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public interface CatalogDef
{
    @NotNull Collection<HierarchyDef> hierarchyDefs();

    HierarchyDef hierarchyDef(String name);

    @NotNull UUID globalId();

    @NotNull CatalogType type();
}
