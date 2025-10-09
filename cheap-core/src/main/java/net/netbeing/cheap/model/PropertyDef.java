/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.model;

import com.google.common.hash.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Defines the metadata and constraints for a property within the Cheap data model.
 * A property definition specifies the name, type, and access characteristics of
 * a property that can be associated with aspects.
 * 
 * <p>In the Cheap model, properties represent the atomic units of data,
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
     * Generate a Cheap-specific SHA-256 hash of this PropertyDef.
     * This hash should be consistent across all Cheap implementations.
     *
     * <P>Implementations of this interface should probably cache the result of this
     * default method for improved performance.</P>
     *
     * @return a HashCode
     */
    @SuppressWarnings("UnstableApiUsage")
    default HashCode hash()
    {
        //TODO: replace use of Hasher with language-independent algo
        Hasher hasher = Hashing.sha256().newHasher();
        hasher.putObject(this, FUNNEL);
        return hasher.hash();
    }

    static Funneler FUNNEL = new Funneler();

    @SuppressWarnings("UnstableApiUsage")
    class Funneler implements Funnel<PropertyDef>
    {
        @Override
        public void funnel(PropertyDef def, PrimitiveSink into)
        {
            into.putBoolean(Objects.requireNonNull(def).isReadable());
            into.putBoolean(def.isWritable());
            into.putBoolean(def.isNullable());
            into.putBoolean(def.isRemovable());
            into.putBoolean(def.hasDefaultValue());
            if (def.hasDefaultValue() && def.defaultValue() != null) {
                into.putInt(def.defaultValue().hashCode());
            }
            into.putString(def.type().typeCode(), UTF_8);
            into.putString(def.name(), UTF_8);
        }
    }

    /**
     * Validates that a property value is compatible with this property definition.
     *
     * <p>For multivalued properties (where {@link #isMultivalued()} returns true), the value
     * must be a List, and each element in the list is validated against the property type.
     * For single-valued properties, the value itself is validated against the property type.</p>
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

            if (isMultivalued()) {
                // For multivalued properties, expect a List
                if (!(value instanceof List<?> listValue)) {
                    if (throwExceptions) {
                        throw new IllegalArgumentException("Property '" + name() + "' is multivalued and expects a List but got "
                            + value.getClass().getSimpleName());
                    }
                    return false;
                }

                // Because of type erasure, the only way to validate the correctness of
                // the type of the list is to examine each element. For performance reasons,
                // we examine only the first element, if there is one.
                if (!listValue.isEmpty()) {
                    Object element = listValue.getFirst();
                    if (element != null && !expectedJavaClass.isAssignableFrom(element.getClass())) {
                        if (throwExceptions) {
                            throw new IllegalArgumentException("Property '" + name() + "' expects List<"
                                + expectedJavaClass.getSimpleName() + "> but the first element is "
                                + element.getClass().getSimpleName());
                        }
                        return false;
                    }
                }
            } else {
                // For single-valued properties, validate the value directly
                if (!expectedJavaClass.isAssignableFrom(value.getClass())) {
                    if (throwExceptions) {
                        throw new IllegalArgumentException("Property '" + name() + "' expects type "
                            + expectedJavaClass.getSimpleName() + " but got " + value.getClass().getSimpleName());
                    }
                    return false;
                }
            }
        }
        return true;
    }

}
