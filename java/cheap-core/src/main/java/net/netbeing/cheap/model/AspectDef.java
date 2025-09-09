package net.netbeing.cheap.model;

import java.util.Collection;

/**
 * The interface Aspect def.
 */
public interface AspectDef
{
    /**
     * Name string.
     *
     * @return the string
     */
    String name();

    /**
     * Property defs collection.
     *
     * @return the collection
     */
    Collection<? extends PropertyDef> propertyDefs();

    /**
     * Property def property def.
     *
     * @param name the name
     * @return the property def
     */
    PropertyDef propertyDef(String name);

    /**
     * Is readable boolean.
     *
     * @return the boolean
     */
    default boolean isReadable()
    {
        return true;
    }

    /**
     * Is writable boolean.
     *
     * @return the boolean
     */
    default boolean isWritable()
    {
        return true;
    }

    /**
     * Can add properties boolean.
     *
     * @return the boolean
     */
    default boolean canAddProperties()
    {
        return true;
    }

    /**
     * Can remove properties boolean.
     *
     * @return the boolean
     */
    default boolean canRemoveProperties()
    {
        return true;
    }
}
