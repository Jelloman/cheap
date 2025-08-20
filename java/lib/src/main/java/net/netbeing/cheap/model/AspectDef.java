package net.netbeing.cheap.model;

import java.util.Collection;

public interface AspectDef
{
    String name();

    Collection<? extends PropertyDef> propertyDefs();

    PropertyDef propertyDef(String name);

    default boolean isReadable()
    {
        return true;
    }

    default boolean isWritable()
    {
        return true;
    }

    default boolean canAddProperties()
    {
        return true;
    }

    default boolean canRemoveProperties()
    {
        return true;
    }
}
