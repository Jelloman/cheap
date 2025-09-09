package net.netbeing.cheap.model;

/**
 * The interface Hierarchy def.
 */
public interface HierarchyDef
{
    /**
     * Name string.
     *
     * @return the string
     */
    String name();

    /**
     * Type hierarchy type.
     *
     * @return the hierarchy type
     */
    HierarchyType type();

    /**
     * Is modifiable boolean.
     *
     * @return the boolean
     */
    default boolean isModifiable()
    {
        return true;
    }

    /**
     * Is immutable boolean.
     *
     * @return the boolean
     */
    default boolean isImmutable()
    {
        return false;
    }
}
