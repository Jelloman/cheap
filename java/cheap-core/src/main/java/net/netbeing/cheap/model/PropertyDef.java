package net.netbeing.cheap.model;

public interface PropertyDef
{
    String name();

    PropertyType type();

    boolean isReadable();

    boolean isWritable();

    boolean isNullable();

    boolean isRemovable();

    boolean isMultivalued();
}
