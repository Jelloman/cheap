package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;

import java.lang.reflect.RecordComponent;
import java.util.*;

public record RecordPropertyDef(
        String name,
        PropertyType type,
        RecordComponent field,
        boolean isNullable,
        boolean isMultivalued
) implements PropertyDef
{
    public RecordPropertyDef
    {
        if (name.isEmpty()) { // implicitly tests for null also
            throw new IllegalArgumentException("Property names must have at least 1 character.");
        }
        Objects.requireNonNull(type);
        Objects.requireNonNull(field);
    }

    public RecordPropertyDef(RecordComponent field)
    {
        this(field.getName(), CheapReflectionUtil.typeOf(field), field, CheapReflectionUtil.nullabilityOf(field), CheapReflectionUtil.isMultivalued(field));
    }

    @Override
    public boolean isReadable()
    {
        return true;
    }

    @Override
    public boolean isWritable()
    {
        return false;
    }

    @Override
    public boolean isRemovable()
    {
        return false;
    }
}
