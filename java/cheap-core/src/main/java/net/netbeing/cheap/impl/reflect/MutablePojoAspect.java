package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;
import org.jetbrains.annotations.NotNull;

public class MutablePojoAspect<P> implements Aspect
{
    private final Catalog catalog;
    private final Entity entity;
    private final MutablePojoAspectDef def;
    private final P object;

    public MutablePojoAspect(@NotNull Catalog catalog, @NotNull Entity entity, @NotNull MutablePojoAspectDef def, @NotNull P object)
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
            throw new IllegalArgumentException("Class " + def.name() + " does not contain field '" + propName + "'.");
        }
        return getter.get(object);
    }

    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        GenericGetterSetter setter = def.setter(propName);
        if (setter == null) {
            throw new IllegalArgumentException("Class " + def.name() + " does not contain field '" + propName + "'.");
        }
        PojoPropertyDef propDef = (PojoPropertyDef) def.propertyDef(propName);
        if (propDef.isJavaPrimitive()) {
            // Handle primitive types by calling the appropriate setter method
            switch (value) {
                case Integer i -> setter.set(object, (int) value);
                case Long l -> setter.set(object, (long) value);
                case Double v -> setter.set(object, (double) value);
                case Float v -> setter.set(object, (float) value);
                case Boolean b -> setter.set(object, (boolean) value);
                case Byte b -> setter.set(object, (byte) value);
                case Short i -> setter.set(object, (short) value);
                case Character c -> setter.set(object, (char) value);
                default -> throw new IllegalStateException("Property '" + propName + "' is flagged as primitive but is not of a primitive type.");
            }
        } else {
            // For non-primitive types and null values
            setter.set(object, value);
        }
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
