package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

public abstract class AspectBaseImpl implements Aspect
{
    protected Catalog catalog;
    protected Entity entity;
    protected AspectDef def;

    public AspectBaseImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def)
    {
        this.catalog = catalog;
        this.entity = entity;
        this.def = def;
    }

    public AspectBaseImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity)
    {
        this.catalog = catalog;
        this.entity = entity;
        this.def = def;
    }

    public AspectBaseImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity, float loadFactor)
    {
        this.catalog = catalog;
        this.entity = entity;
        this.def = def;
    }

    @Override
    public Catalog catalog()
    {
        return catalog;
    }

    @Override
    public Entity entity()
    {
        return entity;
    }

    @Override
    public AspectDef def()
    {
        return def;
    }
}
