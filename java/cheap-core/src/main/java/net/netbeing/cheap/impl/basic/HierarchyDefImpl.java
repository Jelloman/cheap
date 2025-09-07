package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;

import java.util.Objects;

public record HierarchyDefImpl(
        String name,
        HierarchyType type,
        boolean isModifiable,
        boolean isImmutable) implements HierarchyDef
{
    public HierarchyDefImpl
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
    }

    public HierarchyDefImpl(String name, HierarchyType type)
    {
        this(name, type, true, true);
    }
}
