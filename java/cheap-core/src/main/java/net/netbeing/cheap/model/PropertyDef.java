package net.netbeing.cheap.model;

/**
 * Defines the metadata and constraints for a property within the CHEAP data model.
 * A property definition specifies the name, type, and access characteristics of
 * a property that can be associated with aspects.
 * 
 * <p>In the CHEAP model, properties represent the atomic units of data,
 * similar to columns in a database table or instance variables in objects.
 * The PropertyDef serves as the schema definition that determines how property
 * values are stored, accessed, and validated.</p>
 */
public interface PropertyDef
{
    /**
     * Returns the unique name identifier for this property definition.
     * 
     * @return the property name, never null
     */
    String name();

    /**
     * Returns the data type of this property.
     * The type determines what kind of values can be stored in properties
     * created from this definition.
     * 
     * @return the property type, never null
     */
    PropertyType type();

    /**
     * Returns the default value of this property, which may be null.
     * Defaults to null.
     *
     * @return the default value
     */
    default Object defaultValue() {
        return null;
    }

    /**
     * Determines whether this property has a default value. This allows for
     * a difference between not having a default value and having a default value
     * of null. Defaults to false.
     *
     * @return true if the default value should be used
     */
    default boolean hasDefaultValue() {
        return false;
    }

    /**
     * Determines whether properties of this type can be read. Defaults to true.
     * 
     * @return true if the property can be read, false otherwise
     */
    default boolean isReadable() {
        return true;
    }

    /**
     * Determines whether properties of this type can be written or modified.
     * 
     * @return true if the property can be written, false otherwise
     */
    boolean isWritable();

    /**
     * Determines whether properties of this type can have null values.
     * 
     * @return true if null values are allowed, false if the property is required
     */
    boolean isNullable();

    /**
     * Determines whether properties of this type can be removed from their parent aspect.
     * 
     * @return true if the property can be removed, false if it is mandatory
     */
    boolean isRemovable();

    /**
     * Determines whether properties of this type can hold multiple values.
     * A multivalued property can contain a collection of values rather than a single value.
     * 
     * @return true if the property can hold multiple values, false if it holds a single value
     */
    boolean isMultivalued();
}
