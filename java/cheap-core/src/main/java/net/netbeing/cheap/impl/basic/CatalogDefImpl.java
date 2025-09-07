package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CatalogDefImpl implements CatalogDef
{
    private final CatalogType type;
    private final UUID globalId;
    private final Map<String,HierarchyDef> hierarchyDefs = new LinkedHashMap<>(4);

    public CatalogDefImpl()
    {
        this(CatalogType.MIRROR, UUID.randomUUID());
    }

    public CatalogDefImpl(CatalogType type)
    {
        this(type, UUID.randomUUID());
    }

    public CatalogDefImpl(CatalogType type, UUID globalId)
    {
        this.type = type;
        this.globalId = globalId;

        hierarchyDefs.put(CatalogDefaultHierarchies.CATALOG_ROOT_NAME, CatalogDefaultHierarchies.CATALOG_ROOT);
        hierarchyDefs.put(CatalogDefaultHierarchies.ASPECTAGE_NAME, CatalogDefaultHierarchies.ASPECTAGE);
    }

    @Override
    public @NotNull CatalogType type()
    {
        return type;
    }

    @Override
    public @NotNull UUID globalId()
    {
        return globalId;
    }

    @Override
    public @NotNull Collection<HierarchyDef> hierarchyDefs()
    {
        return Collections.unmodifiableCollection(hierarchyDefs.values());
    }

    @Override
    public HierarchyDef hierarchyDef(String name)
    {
        return hierarchyDefs.get(name);
    }
}
