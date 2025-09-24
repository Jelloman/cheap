package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
    @NotNull String name();

    /**
     * Returns the data type of this property.
     * The type determines what kind of values can be stored in properties
     * created from this definition.
     * 
     * @return the property type, never null
     */
    @NotNull PropertyType type();

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
    default boolean isNullable() {
        return false;
    }

    /**
     * Determines whether properties of this type can be removed from their parent aspect.
     * 
     * @return true if the property can be removed, false if it is mandatory
     */
    default boolean isRemovable() {
        return false;
    }

    /**
     * Determines whether properties of this type can hold multiple values.
     * A multivalued property can contain a collection of values rather than a single value.
     * 
     * @return true if the property can hold multiple values, false if it holds a single value
     */
    default boolean isMultivalued() {
        return false;
    }

    /**
     * Perform a full comparison of every field of this PropertyDef.
     * Normal equals() compares only by name, for performance reasons.
     *
     * @return true if the other PropertyDef is fully identical to this one
     */
    default boolean fullyEquals(PropertyDef other)
    {
        return other != null &&
            hasDefaultValue() == other.hasDefaultValue() &&
            isReadable() == other.isReadable() &&
            isWritable() == other.isWritable() &&
            isNullable() == other.isNullable() &&
            isRemovable() == other.isRemovable() &&
            Objects.equals(defaultValue(), other.defaultValue()) &&
            type().equals(other.type()) &&
            name().equals(other.name());
    }

    /**
     * Validates that a property value is compatible with this property definition.
     *
     * @param value the value to validate
     * @param throwExceptions whether this method should throw exceptions or merely return true/false
     * @throws IllegalArgumentException if the value is invalid for the property definition, and throwExceptions is true
     */
    default boolean validatePropertyValue(Object value, boolean throwExceptions)
    {
        // Check nullability
        if (value == null && !isNullable()) {
            if (throwExceptions) {
                throw new IllegalArgumentException("Property '" + name() + "' does not allow null values");
            }
            return false;
        }

        // Check type compatibility if value is not null
        if (value != null) {
            PropertyType expectedType = type();
            Class<?> expectedJavaClass = expectedType.getJavaClass();

            if (!expectedJavaClass.isAssignableFrom(value.getClass())) {
                if (throwExceptions) {
                    throw new IllegalArgumentException("Property '" + name() + "' expects type "
                        + expectedJavaClass.getSimpleName() + " but got " + value.getClass().getSimpleName());
                }
                return false;
            }
        }
        return true;
    }

}
