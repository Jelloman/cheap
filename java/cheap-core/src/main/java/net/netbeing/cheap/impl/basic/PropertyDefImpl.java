package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Record-based implementation of PropertyDef that defines the structure and
 * constraints of a property in the CHEAP system.
 * <p>
 * This implementation uses Java records to provide an immutable property
 * definition with automatic equals, hashCode, and toString implementations.
 * Property definitions specify type information, access permissions, and
 * value constraints.
 * 
 * @param name the name of the property
 * @param type the data type of the property
 * @param isReadable whether the property can be read
 * @param isWritable whether the property can be written
 * @param isNullable whether the property accepts null values
 * @param isRemovable whether the property can be removed
 * @param isMultivalued whether the property can hold multiple values
 * 
 * @see PropertyDef
 * @see PropertyType
 * @see PropertyImpl
 */
public record PropertyDefImpl(
        String name,
        PropertyType type,
        Object defaultValue,
        boolean hasDefaultValue,
        boolean isReadable,
        boolean isWritable,
        boolean isNullable,
        boolean isRemovable,
        boolean isMultivalued
) implements PropertyDef
{
    /**
     * Compact constructor that validates the property definition parameters.
     * 
     * @throws IllegalArgumentException if the name is empty
     * @throws NullPointerException if the type is null
     */
    public PropertyDefImpl
    {
        Objects.requireNonNull(type);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Property names must have at least 1 character.");
        }
        name = name.intern();
    }

    /**
     * Creates a PropertyDefImpl with default settings (no default value, readable,
     * writable, nullable, removable, single-valued).
     * 
     * @param name the name of the property
     * @param type the data type of the property
     */
    public PropertyDefImpl(String name, PropertyType type)
    {
        this(name, type, null, false, true, true, true, true, false);
    }

    /**
     * Creates a PropertyDefImpl with the specified properties and no default value.
     *
     * @param name the name of the property
     * @param type the data type of the property
     * @param isNullable whether the property accepts null values
     * @param isRemovable whether the property can be removed
     */
    public PropertyDefImpl(String name, PropertyType type, boolean isReadable, boolean isWritable,
                           boolean isNullable, boolean isRemovable, boolean isMultivalued
    )
    {
        this(name, type, null, false, isReadable, isWritable, isNullable, isRemovable, isMultivalued);
    }

    /**
     * Creates a read-only PropertyDefImpl with specified nullable and removable settings,
     * defaulting to (no default value, readable, writable, and single-valued)
     *
     * @param name the name of the property
     * @param type the data type of the property
     * @param isNullable whether the property accepts null values
     * @param isRemovable whether the property can be removed
     * @return a new read-only PropertyDefImpl instance
     */
    public static @NotNull PropertyDefImpl readOnly(String name, PropertyType type, boolean isNullable, boolean isRemovable)
    {
        return new PropertyDefImpl(name, type, null, false, true, false, isNullable, isRemovable, false);
    }

    /**
     * Tests whether this is equal to another object. True if the other is a PropertyDef
     * and has the same name.
     *
     * @param other the reference object with which to compare.
     * @return true if the other is a PropertyDef and has the same name
     */
    public boolean equals(Object other)
    {
        return (other instanceof PropertyDef) && name == ((PropertyDef) other).name();
    }

    /**
     * Generate a hashcode based only on name.
     *
     * @return hashcode based only on name
     */
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
}
