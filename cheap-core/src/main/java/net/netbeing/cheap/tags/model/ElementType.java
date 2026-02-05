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

package net.netbeing.cheap.tags.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enumerates the five fundamental element types in the CHEAP data model that can be tagged.
 *
 * <p>The CHEAP acronym represents a hierarchical data structure:</p>
 * <ul>
 *   <li><b>C</b>atalog - Database equivalent / Volume</li>
 *   <li><b>H</b>ierarchy - Table or Index / Directory structure</li>
 *   <li><b>E</b>ntity - Primary Key / File, file element</li>
 *   <li><b>A</b>spect - Row / File or element attributes or content</li>
 *   <li><b>P</b>roperty - Column / Single attribute or content atom</li>
 * </ul>
 *
 * <p>Each element type can have tags applied to it, enabling semantic metadata
 * to be attached at any level of the data model hierarchy.</p>
 *
 * @see net.netbeing.cheap.model.Catalog
 * @see net.netbeing.cheap.model.Hierarchy
 * @see net.netbeing.cheap.model.Entity
 * @see net.netbeing.cheap.model.Aspect
 * @see net.netbeing.cheap.model.Property
 */
public enum ElementType
{
    /**
     * Property - the finest-grained element representing a single attribute or value.
     * Corresponds to a column in a database table or a single field in an object.
     */
    PROPERTY,

    /**
     * Aspect - a collection of related properties that describe an entity.
     * Corresponds to a row in a database table or an object's attributes.
     */
    ASPECT,

    /**
     * Hierarchy - a collection or structure of entities.
     * Corresponds to a table, index, or directory structure.
     */
    HIERARCHY,

    /**
     * Entity - a uniquely identifiable data element that can have multiple aspects.
     * Corresponds to a primary key or a file.
     */
    ENTITY,

    /**
     * Catalog - the top-level container for hierarchies and entities.
     * Corresponds to a database or volume.
     */
    CATALOG;

    /**
     * Converts a string representation to an ElementType enum value.
     *
     * <p>This method is case-insensitive and accepts both uppercase and lowercase strings.</p>
     *
     * @param value the string value to convert (e.g., "property", "PROPERTY", "Property")
     * @return the corresponding ElementType enum value, or null if the string doesn't match any type
     */
    @Nullable
    public static ElementType fromString(@Nullable String value)
    {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return ElementType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the lowercase string representation of this element type.
     *
     * <p>This is useful for generating human-readable or API-friendly names.</p>
     *
     * @return the lowercase name (e.g., "property", "aspect", "hierarchy")
     */
    @NotNull
    public String toLowerString()
    {
        return name().toLowerCase();
    }
}
