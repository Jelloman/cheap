package net.netbeing.cheap.model;

import net.netbeing.cheap.impl.basic.PropertyImpl;
import org.jetbrains.annotations.NotNull;

public interface Aspect
{
    Entity entity();

    AspectDef def();

    Catalog catalog();

    Object unsafeReadObj(@NotNull String propName);

    void unsafeWrite(@NotNull String propName, Object value);

    void unsafeAdd(@NotNull Property prop);

    void unsafeRemove(@NotNull String propName);

    default boolean contains(@NotNull String propName)
    {
        return unsafeReadObj(propName) != null;
    }

    @SuppressWarnings("unchecked")
    default <T> T uncheckedRead(@NotNull String propName)
    {
        //noinspection unchecked
        return (T) readObj(propName);
    }

    @SuppressWarnings("unchecked")
    default <T> T unsafeRead(@NotNull String propName)
    {
        //noinspection unchecked
        return (T) unsafeReadObj(propName);
    }

    default Object readObj(@NotNull String propName)
    {
        AspectDef def = def();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Aspect '" + def.name() + "' is not readable.");
        }
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            throw new IllegalArgumentException("Aspect '" + def.name() + "' does not contain prop named '" + propName + "'.");
        }
        if (!propDef.isReadable()) {
            throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + def.name() + "' is not readable.");
        }
        return unsafeReadObj(propName);
    }

    default <T> T readAs(@NotNull String propName, @NotNull Class<T> type)
    {
        AspectDef def = def();
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            throw new IllegalArgumentException("Aspect '" + def.name() + "' does not contain prop named '" + propName + "'.");
        }
        if (!type.isAssignableFrom(propDef.type().getJavaClass())) {
            throw new ClassCastException("Property '" + propName + "' with type '" + propDef.type().name() + "' cannot be cast to type " + type.getTypeName() + ".");
        }
        Object objVal = unsafeReadObj(propName);
        return type.cast(objVal);
    }

    default Property get(@NotNull String propName)
    {
        AspectDef def = def();
        String name = def.name();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Aspect '" + name + "' is not readable.");
        }
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            throw new IllegalArgumentException("Aspect '" + name + "' does not contain prop named '" + propName + "'.");
        }
        if (!propDef.isReadable()) {
            throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + name + "' is not readable.");
        }
        return new PropertyImpl(propDef, unsafeReadObj(propName));
    }

    default void put(@NotNull Property prop)
    {
        AspectDef def = def();
        String name = def.name();
        String propName = prop.def().name();
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            if (!def.canAddProperties()) {
                throw new IllegalArgumentException("Aspect '" + name + "' does not support adding properties.");
            }
            unsafeAdd(prop);
        } else {
            if (!def.isWritable()) {
                throw new UnsupportedOperationException("Aspect '" + name + "' is not writable.");
            }
            if (!propDef.isWritable()) {
                throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + name + "' is not writable.");
            }
            unsafeWrite(propName, prop.unsafeRead());
        }
    }

    default void remove(@NotNull Property prop)
    {
        AspectDef def = def();
        String name = def.name();
        if (!def.canRemoveProperties()) {
            throw new UnsupportedOperationException("Aspect '" + name + "' does not support property removal.");
        }
        PropertyDef propDef = prop.def();
        String propName = propDef.name();
        Property currProp = get(propName);
        if (currProp == null) {
            throw new IllegalArgumentException("Aspect '" + name + "' does not contain a property named '" + propName + "'.");
        }
        PropertyDef currDef = currProp.def();
        if (currDef != propDef) { //TODO: use Entities.equal after writing it
            throw new ClassCastException("PropertyDef '" + propName + "' is not equal to existing PropertyDef '" + currDef.name() + "' in Aspect '" + name + "'.");
        }
        if (!currDef.isRemovable()) {
            throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + name + "' is not removable.");
        }
        // TODO: Should value be checked for equality also? It may seem more "correct" but what's the benefit? It also prevents removal of unreadable properties.
        unsafeRemove(propName);
    }

    default void write(@NotNull String propName, Object value)
    {
        AspectDef def = def();
        String name = def.name();
        if (!def.isWritable()) {
            throw new UnsupportedOperationException("Aspect '" + name + "' is not writable.");
        }
        Property currProp = get(propName);
        if (currProp == null) {
            throw new IllegalArgumentException("Aspect '" + name + "' does not contain prop named '" + propName + "'.");
        }
        PropertyDef currDef = currProp.def();
        if (!currDef.isWritable()) {
            throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + name + "' is not writable.");
        }
        if (value == null && !currDef.isNullable()) {
            throw new NullPointerException("Property '" + propName + "' in Aspect '" + name + "' is not nullable.");
        }
        unsafeWrite(propName, value);
    }

    default void putAll(@NotNull Iterable<Property> properties)
    {
        for (Property prop : properties) {
            put(prop);
        }
    }

    default void unsafeWriteAll(@NotNull Iterable<Property> properties)
    {
        for (Property prop : properties) {
            PropertyDef def = prop.def();
            unsafeWrite(def.name(), prop.unsafeRead());
        }
    }
}
