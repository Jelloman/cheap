package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

/**
 * Individual, immutable Property object.
 */
public interface Property
{
    PropertyDef def();

    Object unsafeRead();

    @SuppressWarnings("unchecked")
    default <T> T unsafeReadAs()
    {
        //noinspection unchecked
        return (T) read();
    }

    default Object read()
    {
        PropertyDef def = def();
        String name = def.name();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Property '" + name + "' is not readable.");
        }
        return unsafeRead();
    }

    default <T> T readAs(@NotNull Class<T> type)
    {
        PropertyDef def = def();
        String name = def.name();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Property '" + name + "' is not readable.");
        }
        if (!type.isAssignableFrom(def.type().getJavaClass())) {
            throw new ClassCastException("Property '" + name + "' of type '" + def.type().getJavaClass().getTypeName() + "' cannot be assigned to type '" + type.getTypeName() + "'.");
        }
        return type.cast(unsafeRead());
    }
}
