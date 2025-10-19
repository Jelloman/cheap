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

import net.netbeing.cheap.util.CheapHasher;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Defines the structure and metadata for an aspect type within the Cheap data model.
 * An aspect definition specifies the properties that can be associated with entities
 * and controls the read/write capabilities and mutability of those properties.
 * 
 * <p>In the Cheap model, aspects represent collections of properties that can be
 * attached to entities, similar to rows in a database table or attributes of a file.
 * The AspectDef serves as the schema definition for these aspects.</p>
 */
public interface AspectDef extends Entity
{
    /**
     * Returns the unique name identifier for this aspect definition.
     * 
     * @return the aspect name, never null
     */
    @NotNull String name();

    /**
     * Returns all property definitions that belong to this aspect.
     * The collection provides access to the complete schema of properties
     * that can be associated with aspects of this type.
     * 
     * @return a collection of property definitions, never null but may be empty
     */
    @NotNull Collection<PropertyDef> propertyDefs();

    /**
     * Return the number of properties in this AspectDef.
     *
     * @return the number of properties in this AspectDef
     */
    default int size() {
        return propertyDefs().size();
    }

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
     * This controls the mutability of the aspect at runtime, but it does NOT control the
     * mutability of the AspectDef itself; the addition of a new Property to an Aspect of
     * this type must not modify this AspectDef. (Extension of an AspectDef may be possible,
     * but it is implementation-dependent and not done via this interface.)
     * 
     * @return true if properties can be added, false otherwise; defaults to false
     */
    default boolean canAddProperties()
    {
        return false;
    }

    /**
     * Determines whether properties can be dynamically removed from aspects of this type.
     * This controls the mutability of the aspect schema at runtime.
     * 
     * @return true if properties can be removed, false otherwise; defaults to false
     */
    default boolean canRemoveProperties()
    {
        return false;
    }

    /**
     * Perform a full comparison of every field of this AspectDef.
     * Normal equals() compares only by name, for performance reasons.
     *
     * @return true if the other AspectDef is fully identical to this one
     */
    default boolean fullyEquals(AspectDef other)
    {
        if (!globalId().equals(other.globalId()) ||
            !name().equals(other.name()) ||
            isReadable() != other.isReadable() ||
            isWritable() != other.isWritable() ||
            canAddProperties() != other.canAddProperties() ||
            canRemoveProperties() != other.canRemoveProperties()) {
            return false;
        }
        for (PropertyDef propDef : propertyDefs()) {
            PropertyDef otherPropDef = other.propertyDef(propDef.name());
            if (otherPropDef == null || !propDef.fullyEquals(otherPropDef)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generate a Cheap-specific FNV-1a hash of this AspectDef.
     * This hash should be consistent across all Cheap implementations.
     *
     * <P>Implementations of this interface should probably cache the result of this
     * default method for improved performance.</P>
     *
     * @return a 64-bit hash value
     */
    default long hash()
    {
        CheapHasher hasher = new CheapHasher();
        hasher.update(isReadable());
        hasher.update(isWritable());
        hasher.update(canAddProperties());
        hasher.update(canRemoveProperties());
        hasher.update(name());
        hasher.update(globalId());
        for (PropertyDef pDef : propertyDefs()) {
            hasher.update(pDef.hash());
        }
        return hasher.getHash();
    }

}
