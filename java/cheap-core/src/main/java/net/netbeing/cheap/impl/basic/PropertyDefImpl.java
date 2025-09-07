package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record PropertyDefImpl(
        String name,
        PropertyType type,
        boolean isReadable,
        boolean isWritable,
        boolean isNullable,
        boolean isRemovable,
        boolean isMultivalued
) implements PropertyDef
{
    public PropertyDefImpl
    {
        Objects.requireNonNull(type);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Property names must have at least 1 character.");
        }
    }

    public PropertyDefImpl(String name, PropertyType type)
    {
        this(name, type, true, true, true, true, false);
    }

    public static @NotNull PropertyDefImpl readOnly(String name, PropertyType type, boolean isNullable, boolean isRemovable)
    {
        return new PropertyDefImpl(name, type, true, false, isNullable, isRemovable, false);
    }

}
