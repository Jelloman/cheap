package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.impl.basic.EntityLazyIdImpl;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;
import tech.hiddenproject.aide.reflection.LambdaWrapper;

public class RecordAspect<R extends Record> implements Aspect
{
    private final Catalog catalog;
    private final Entity entity;
    private final RecordAspectDef def;
    private final R record;

    public RecordAspect(@NotNull Catalog catalog, @NotNull RecordAspectDef def, @NotNull R record)
    {
        this.catalog = catalog;
        this.entity = new EntityLazyIdImpl(this);
        this.def = def;
        this.record = record;
    }

    public RecordAspect(@NotNull Catalog catalog, @NotNull Entity entity, @NotNull RecordAspectDef def, @NotNull R record)
    {
        this.catalog = catalog;
        this.entity = entity;
        this.def = def;
        this.record = record;
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

    public R record()
    {
        return record;
    }

    @Override
    public Object unsafeReadObj(@NotNull String propName)
    {
        LambdaWrapper getter = def.getter(propName);
        if (getter == null) {
            throw new IllegalArgumentException("Class " + def.name() + " does not contain field '" + propName + ".");
        }
        return getter.get(record);
    }

    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be set in Record class with immutable AspectDef '" + def.name() + "'.");
    }

    @Override
    public void unsafeAdd(@NotNull Property prop)
    {
        throw new UnsupportedOperationException("Property '" + prop.def().name() + "' cannot be added to Record class with immutable AspectDef '" + def.name() + "'.");
    }

    @Override
    public void unsafeRemove(@NotNull String propName)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be removed in Record class with immutable AspectDef '" + def.name() + "'.");
    }
}
