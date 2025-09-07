package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class AspectObjectMapImpl extends AspectBaseImpl
{
    protected Map<String, Object> props;

    public AspectObjectMapImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def)
    {
        super(catalog, entity, def);
        this.props = new LinkedHashMap<>();
    }

    public AspectObjectMapImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity)
    {
        super(catalog, entity, def);
        this.props = new LinkedHashMap<>(initialCapacity);
    }

    public AspectObjectMapImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity, float loadFactor)
    {
        super(catalog, entity, def);
        this.props = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    @Override
    public boolean contains(@NotNull String propName)
    {
        return props.containsKey(propName);
    }

    @Override
    public Object unsafeReadObj(@NotNull String propName)
    {
        return props.get(propName);
    }

    @Override
    public void unsafeAdd(@NotNull Property prop)
    {
        props.put(prop.def().name(), prop);
    }

    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        if (!props.containsKey(propName)) {
            throw new IllegalArgumentException("Aspect '" + def().name() + "' does not contain prop named '" + propName + "'");
        }
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            throw new IllegalArgumentException("Aspect '" + def.name() + "' does not contain prop named '" + propName + "'.");
        }

        Property newProp = new PropertyImpl(propDef, value);
        props.put(propName, newProp);
    }

    @Override
    public void unsafeRemove(@NotNull String propName)
    {
        props.remove(propName);
    }
}
