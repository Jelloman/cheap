package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;

public class ImmutablePojoAspect<P> implements Aspect
{
    private final Catalog catalog;
    private final Entity entity;
    private final ImmutablePojoAspectDef def;
    private final P object;

    public ImmutablePojoAspect(@NotNull Catalog catalog, @NotNull Entity entity, @NotNull ImmutablePojoAspectDef def, @NotNull P object)
    {
        this.catalog = catalog;
        this.entity = entity;
        this.def = def;
        this.object = object;

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

    public P object()
    {
        return object;
    }

    @Override
    public Object unsafeReadObj(@NotNull String propName)
    {
        GenericGetterSetter getter = def.getter(propName);
        if (getter == null) {
            throw new IllegalArgumentException("Class " + def.name() + " does not contain field '" + propName + ".");
        }
        return getter.get(object);
    }

    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be set in Record class with immutable AspectDef '" + def.name() + "'.");
    }

    @Override
    public void unsafeAdd(@NotNull Property prop)
    {
        throw new UnsupportedOperationException("Property '" + prop.def().name() + "' cannot be added to Java class with AspectDef '" + def.name() + "'.");
    }

    @Override
    public void unsafeRemove(@NotNull String propName)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be removed from Java class with AspectDef '" + def.name() + "'.");
    }
}
