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

import org.jetbrains.annotations.NotNull;

/**
 * Represents an individual, immutable property within an aspect, serving as the "P"
 * in the Cheap acronym (Catalog, Hierarchy, Entity, Aspect, Property). A Property
 * combines a value with its definition and provides type-safe access to the data.
 * 
 * <p>Properties are analogous to columns in database terminology or individual
 * attributes in object-oriented programming. Each property has a definition that
 * specifies its type, constraints, and access permissions.</p>
 * 
 * <p>Properties are immutable once created - their values cannot be changed through
 * this interface. To modify property values, new Property instances must be created
 * and applied to their containing Aspect.</p>
 */
public interface Property
{
    /**
     * Returns the property definition that describes this property's type,
     * constraints, and access permissions.
     * 
     * <p>The property definition serves as the schema for this property instance,
     * defining the expected type and validation rules for its value.</p>
     *
     * @return the property definition for this property, never null
     */
    PropertyDef def();

    /**
     * Reads the property value without performing any validation or security checks.
     * This method provides direct access to the raw value for maximum performance.
     * 
     * <p>Use with caution - this method bypasses readability constraints and
     * may return unexpected types or values.</p>
     *
     * @return the raw property value, may be null
     */
    Object unsafeRead();

    /**
     * Reads the property value with type casting but without validation.
     * This method bypasses security checks but provides convenient type casting.
     * 
     * <p>Use with caution - this method may result in ClassCastException if
     * the actual value type is incompatible with the requested type.</p>
     *
     * @param <T> the expected type of the property value
     * @return the property value cast to type T, may be null
     * @throws ClassCastException if the value cannot be cast to the requested type
     */
    @SuppressWarnings("unchecked")
    default <T> T unsafeReadAs()
    {
        //noinspection unchecked
        return (T) read();
    }

    /**
     * Reads the property value with full validation against the property definition.
     * This is the safest read method, performing readability checks before returning the value.
     * 
     * <p>This method verifies that the property is readable according to its
     * definition before returning the value.</p>
     *
     * @return the property value as an Object, may be null
     * @throws UnsupportedOperationException if the property is not readable
     */
    default Object read()
    {
        PropertyDef def = def();
        String name = def.name();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Property '" + name + "' is not readable.");
        }
        return unsafeRead();
    }

    /**
     * Reads the property value with full validation and safe type casting.
     * This is the recommended method for reading typed property values safely.
     * 
     * <p>This method performs readability validation and uses Java's type casting
     * mechanisms to ensure type safety. It provides the best balance of safety
     * and usability for typed property access.</p>
     *
     * @param <T>  the expected type of the property value
     * @param type the Class representing the expected type, must not be null
     * @return the property value cast to the specified type, may be null
     * @throws UnsupportedOperationException if the property is not readable
     * @throws ClassCastException if the value cannot be cast to the specified type
     */
    default <T> T readAs(@NotNull Class<T> type)
    {
        PropertyDef def = def();
        String name = def.name();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Property '" + name + "' is not readable.");
        }
        //if (!type.isAssignableFrom(def.type().getJavaClass())) {
        //    throw new ClassCastException("Property '" + name + "' of type '" + def.type().getJavaClass().getTypeName() + "' cannot be assigned to type '" + type.getTypeName() + "'.");
        //}
        return type.cast(unsafeRead());
    }
}
