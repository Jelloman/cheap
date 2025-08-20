package net.netbeing.cheap.model;

public interface HierarchyDef
{
    String name();

    HierarchyType type();

    default boolean isModifiable()
    {
        return true;
    }

    default boolean isImmutable()
    {
        return false;
    }
}
