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
 * Defines the structure and metadata for an aspect type within the Cheap data model.
 * An aspect definition specifies the properties that can be associated with entities
 * and controls the read/write capabilities and mutability of those properties.
 * 
 * <p>In the Cheap model, aspects represent collections of properties that can be
 * attached to entities, similar to rows in a database table or attributes of a file.
 * The AspectDef serves as the schema definition for these aspects.</p>
 */
public interface MutableAspectDef extends AspectDef
{
    /**
     * Determines whether new properties can be dynamically added to aspects of this type.
     * This controls the mutability of the aspect at runtime, but it does NOT control the
     * mutability of the AspectDef itself; the addition of a new Property to an Aspect of
     * this type must not modify this AspectDef. (Extension of an AspectDef may be possible,
     * but it is implementation-dependent and not done via this interface.)
     * 
     * @return true if properties can be added, false otherwise; defaults to false
     */
    @Override
    default boolean canAddProperties()
    {
        return true;
    }

    /**
     * Determines whether properties can be dynamically removed from aspects of this type.
     * This controls the mutability of the aspect schema at runtime.
     * 
     * @return true if properties can be removed, false otherwise; defaults to false
     */
    @Override
    default boolean canRemoveProperties()
    {
        return true;
    }

    /**
     * Adds a property definition to this mutable aspect definition.
     *
     * @param prop the property definition to add
     * @return the previous property definition with the same name, or {@code null} if none existed
     */
    PropertyDef add(@NotNull PropertyDef prop);

    /**
     * Removes a property definition from this mutable aspect definition.
     *
     * @param prop the property definition to remove
     * @return the removed property definition, or {@code null} if it wasn't present
     */
    PropertyDef remove(@NotNull PropertyDef prop);

}
