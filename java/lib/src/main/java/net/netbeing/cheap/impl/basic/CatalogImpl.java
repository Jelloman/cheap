package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

public class CatalogImpl extends EntityFullImpl implements Catalog
{
    private final CatalogDef def;
    private final Catalog upstream;
    private final HierarchyDirImpl hierarchies;
    private final AspectDefDirHierarchyImpl aspectage;

    public CatalogImpl()
    {
        this(new CatalogDefImpl(CatalogType.ROOT), null);
    }

    public CatalogImpl(CatalogDef def)
    {
        this(def, null);
    }

    public CatalogImpl(CatalogDef def, Catalog upstream)
    {
        super(def.globalId());
        this.def = def;
        this.upstream = upstream;

        switch (def.type()) {
            case ROOT -> {
                if (upstream != null) {
                    throw new IllegalStateException("Root catalogs may not have an upstream catalog.");
                }
            }
            case MIRROR -> {
                if (upstream == null) {
                    throw new IllegalStateException("Mirror catalogs must have an upstream catalog.");
                }
            }
        }

        this.hierarchies = new HierarchyDirImpl(CatalogDefaultHierarchies.CATALOG_ROOT);
        this.aspectage = new AspectDefDirHierarchyImpl();
        hierarchies.put(CatalogDefaultHierarchies.CATALOG_ROOT_NAME, hierarchies);
        hierarchies.put(CatalogDefaultHierarchies.ASPECTAGE_NAME, aspectage);
    }

    @Override
    public @NotNull CatalogDef def()
    {
        return def;
    }

    @Override
    public Catalog upstream()
    {
        return upstream;
    }

    @Override
    public @NotNull HierarchyDir hierarchies()
    {
        return hierarchies;
    }

    @Override
    public Hierarchy hierarchy(String name)
    {
        return hierarchies.get(name);
    }

    @Override
    public AspectDef aspectDef(String name)
    {
        // TODO: aspectage
        return aspectage.get(name);
    }

}
