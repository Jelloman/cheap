package net.netbeing.cheap.model;

import java.util.Collection;

/**
 * Defines the structure and metadata for an aspect type within the CHEAP data model.
 * An aspect definition specifies the properties that can be associated with entities
 * and controls the read/write capabilities and mutability of those properties.
 * 
 * <p>In the CHEAP model, aspects represent collections of properties that can be
 * attached to entities, similar to rows in a database table or attributes of a file.
 * The AspectDef serves as the schema definition for these aspects.</p>
 */
public interface AspectDef
{
    /**
     * Returns the unique name identifier for this aspect definition.
     * 
     * @return the aspect name, never null
     */
    String name();

    /**
     * Returns all property definitions that belong to this aspect.
     * The collection provides access to the complete schema of properties
     * that can be associated with aspects of this type.
     * 
     * @return a collection of property definitions, never null but may be empty
     */
    Collection<? extends PropertyDef> propertyDefs();

    /**
     * Retrieves a specific property definition by name.
     * 
     * @param name the name of the property definition to retrieve
     * @return the property definition with the specified name, or null if not found
     */
    PropertyDef propertyDef(String name);

    /**
     * Determines whether aspects of this type can be read.
     * 
     * @return true if aspects can be read, false otherwise; defaults to true
     */
    default boolean isReadable()
    {
        return true;
    }

    /**
     * Determines whether aspects of this type can be written or modified.
     * 
     * @return true if aspects can be written, false otherwise; defaults to true
     */
    default boolean isWritable()
    {
        return true;
    }

    /**
     * Determines whether new properties can be dynamically added to aspects of this type.
     * This controls the mutability of the aspect schema at runtime.
     * 
     * @return true if properties can be added, false otherwise; defaults to true
     */
    default boolean canAddProperties()
    {
        return true;
    }

    /**
     * Determines whether properties can be dynamically removed from aspects of this type.
     * This controls the mutability of the aspect schema at runtime.
     * 
     * @return true if properties can be removed, false otherwise; defaults to true
     */
    default boolean canRemoveProperties()
    {
        return true;
    }
}
