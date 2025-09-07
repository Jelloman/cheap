package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class AspectPropertyMapImpl extends AspectBaseImpl
{
    protected Map<String, Property> props;

    public AspectPropertyMapImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def)
    {
        super(catalog, entity, def);
        this.props = new LinkedHashMap<>();
    }

    public AspectPropertyMapImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity)
    {
        super(catalog, entity, def);
        this.props = new LinkedHashMap<>(initialCapacity);
    }

    public AspectPropertyMapImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity, float loadFactor)
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
        Property prop = props.get(propName);
        return (prop == null) ? null : prop.unsafeRead();
    }

    @Override
    public Property get(@NotNull String propName)
    {
        AspectDef def = def();
        String name = def.name();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Aspect '" + name + "' is not readable.");
        }
        Property prop = props.get(propName);
        if (prop == null) {
            throw new IllegalArgumentException("Aspect '" + name + "'does not contain prop named '" + propName + "'");
        }
        PropertyDef propDef = prop.def();
        if (!propDef.isReadable()) {
            throw new UnsupportedOperationException("Property '" + propDef.name() + "' in Aspect '" + name + "' is not readable.");
        }
        return prop;
    }

    @Override
    public void put(@NotNull Property prop)
    {
        AspectDef def = def();
        String name = def.name();
        if (!def.isWritable()) {
            throw new UnsupportedOperationException("Aspect '" + name + "' is not writable.");
        }
        PropertyDef propDef = prop.def();
        String propName = propDef.name();
        Property currProp = props.get(propName);
        if (currProp == null) {
            if (!def.canAddProperties()) {
                throw new IllegalArgumentException("Aspect '" + name + "' does not contain prop named '" + propName + "' and is not extensible.");
            }
        } else {
            if (!propDef.isWritable()) {
                throw new UnsupportedOperationException("Property '" + propName + "' is not writable.");
            }
            PropertyDef currDef = currProp.def();
            if (currDef != propDef) { //FIXME: use Entities.equal after writing it
                throw new ClassCastException("PropertyDef '" + propName + "' is not equal to existing PropertyDef '" + currDef.name() + "'.");
            }
        }
        props.put(propName, prop);
    }

    @Override
    public void unsafeAdd(@NotNull Property prop)
    {
        props.put(prop.def().name(), prop);
    }

    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        Property prop = props.get(propName);
        if (prop == null) {
            throw new IllegalArgumentException("Aspect '" + def().name() + "' does not contain prop named '" + propName + "'");
        }
        Property newProp = new PropertyImpl(prop.def(), value);
        props.put(propName, newProp);
    }

    @Override
    public void unsafeRemove(@NotNull String propName)
    {
        props.remove(propName);
    }
}
