package net.netbeing.cheap.model;

/**
 * The interface Property def.
 */
public interface PropertyDef
{
    /**
     * Name string.
     *
     * @return the string
     */
    String name();

    /**
     * Type property type.
     *
     * @return the property type
     */
    PropertyType type();

    /**
     * Is readable boolean.
     *
     * @return the boolean
     */
    boolean isReadable();

    /**
     * Is writable boolean.
     *
     * @return the boolean
     */
    boolean isWritable();

    /**
     * Is nullable boolean.
     *
     * @return the boolean
     */
    boolean isNullable();

    /**
     * Is removable boolean.
     *
     * @return the boolean
     */
    boolean isRemovable();

    /**
     * Is multivalued boolean.
     *
     * @return the boolean
     */
    boolean isMultivalued();
}
