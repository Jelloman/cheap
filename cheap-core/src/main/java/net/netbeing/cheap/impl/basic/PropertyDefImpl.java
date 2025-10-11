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

package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Implementation of PropertyDef that defines the structure and
 * constraints of a property in the Cheap system.
 * <p>
 * This implementation provides an immutable property definition with
 * cached hash computation for improved performance.
 * Property definitions specify type information, access permissions, and
 * value constraints.
 *
 * @see PropertyDef
 * @see PropertyType
 * @see PropertyImpl
 */
public final class PropertyDefImpl implements PropertyDef
{
    private final String name;
    private final PropertyType type;
    private final Object defaultValue;
    private final boolean hasDefaultValue;
    private final boolean isReadable;
    private final boolean isWritable;
    private final boolean isNullable;
    private final boolean isRemovable;
    private final boolean isMultivalued;

    /** Cached hash value (0 means not yet computed). */
    private volatile long cachedHash = 0;

    /**
     * Primary constructor that creates a PropertyDefImpl with all parameters.
     *
     * @param name the name of the property
     * @param type the data type of the property
     * @param defaultValue the default value for the property
     * @param hasDefaultValue whether the property has a default value
     * @param isReadable whether the property can be read
     * @param isWritable whether the property can be written
     * @param isNullable whether the property accepts null values
     * @param isRemovable whether the property can be removed
     * @param isMultivalued whether the property can hold multiple values
     *
     * @throws IllegalArgumentException if the name is empty
     * @throws NullPointerException if the type is null
     */
    public PropertyDefImpl(
            String name,
            PropertyType type,
            Object defaultValue,
            boolean hasDefaultValue,
            boolean isReadable,
            boolean isWritable,
            boolean isNullable,
            boolean isRemovable,
            boolean isMultivalued)
    {
        Objects.requireNonNull(type);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Property names must have at least 1 character.");
        }
        this.name = name.intern();
        this.type = type;
        this.defaultValue = defaultValue;
        this.hasDefaultValue = hasDefaultValue;
        this.isReadable = isReadable;
        this.isWritable = isWritable;
        this.isNullable = isNullable;
        this.isRemovable = isRemovable;
        this.isMultivalued = isMultivalued;
    }

    @Override
    public @NotNull String name()
    {
        return name;
    }

    @Override
    public @NotNull PropertyType type()
    {
        return type;
    }

    @Override
    public Object defaultValue()
    {
        return defaultValue;
    }

    @Override
    public boolean hasDefaultValue()
    {
        return hasDefaultValue;
    }

    @Override
    public boolean isReadable()
    {
        return isReadable;
    }

    @Override
    public boolean isWritable()
    {
        return isWritable;
    }

    @Override
    public boolean isNullable()
    {
        return isNullable;
    }

    @Override
    public boolean isRemovable()
    {
        return isRemovable;
    }

    @Override
    public boolean isMultivalued()
    {
        return isMultivalued;
    }

    /**
     * Tests whether this is equal to another object. True if the other is a PropertyDef
     * and has the same name.
     *
     * @param other the reference object with which to compare.
     * @return true if the other is a PropertyDef and has the same name
     */
    @SuppressWarnings("StringEquality")
    @Override
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

    /**
     * {@inheritDoc}
     * <p>
     * This implementation caches the computed hash value for improved performance.
     * The cache uses a volatile long variable with 0 as the sentinel value indicating
     * "not yet computed". This is safe because the FNV-1a hash algorithm never produces
     * a hash value of 0 for any non-empty input.
     */
    @Override
    public long hash()
    {
        long result = cachedHash;
        if (result == 0) {
            // Compute hash using default implementation from PropertyDef interface
            result = PropertyDef.super.hash();
            cachedHash = result;
        }
        return result;
    }

    @Override
    public String toString()
    {
        return "PropertyDefImpl[" +
                "name=" + name +
                ", type=" + type +
                ", defaultValue=" + defaultValue +
                ", hasDefaultValue=" + hasDefaultValue +
                ", isReadable=" + isReadable +
                ", isWritable=" + isWritable +
                ", isNullable=" + isNullable +
                ", isRemovable=" + isRemovable +
                ", isMultivalued=" + isMultivalued +
                ']';
    }
}
