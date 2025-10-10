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

import net.netbeing.cheap.model.MutableAspectDef;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Mutable implementation of an AspectDef that allows modification after creation.
 * This implementation allows adding and removing property definitions dynamically,
 * making it suitable for aspect definitions that need to evolve during runtime.
 * <p>
 * Unlike {@link ImmutableAspectDefImpl}, this class supports modification operations
 * and maintains a mutable internal map of property definitions.
 * 
 * @see AspectDefBase
 * @see ImmutableAspectDefImpl
 * @see PropertyDef
 */
public class MutableAspectDefImpl extends AspectDefBase implements MutableAspectDef
{
    /**
     * Creates a new MutableAspectDefImpl with the specified name and empty property definitions.
     * This default version is NOT threadsafe.
     * 
     * @param name the name of this aspect definition
     */
    public MutableAspectDefImpl(@NotNull String name)
    {
        super(name);
    }

    /**
     * Creates a new MutableAspectDefImpl with the specified name and empty property definitions.
     * This default version is NOT threadsafe.
     *
     * @param name the name of this aspect definition
     */
    public MutableAspectDefImpl(@NotNull String name, @NotNull UUID globalId)
    {
        super(name, globalId);
    }

    /**
     * Creates a new MutableAspectDefImpl with the specified name and property definitions.
     * The propertyDefs map is used directly, not copied. To make a threadsafe version of
     * this class, pass in a threadsafe Map.
     *
     * @param name the name of this aspect definition
     * @param propertyDefs the map of property names to property definitions
     */
    public MutableAspectDefImpl(@NotNull String name, @NotNull Map<String, PropertyDef> propertyDefs)
    {
        this(name, UUID.randomUUID(), propertyDefs);
    }

    /**
     * Creates a new MutableAspectDefImpl with the specified name and property definitions.
     * The propertyDefs map is used directly, not copied. To make a threadsafe version of
     * this class, pass in a threadsafe Map.
     * 
     * @param name the name of this aspect definition
     * @param propertyDefs the map of property names to property definitions
     */
    public MutableAspectDefImpl(@NotNull String name, @NotNull UUID globalId, @NotNull Map<String, PropertyDef> propertyDefs)
    {
        super(name, globalId, propertyDefs);
    }

    /**
     * Returns whether properties can be added to this aspect definition.
     *
     * @return {@code true} as this is a mutable aspect definition
     */
    @Override
    public boolean canAddProperties()
    {
        return true;
    }

    /**
     * Returns whether properties can be removed from this aspect definition.
     *
     * @return {@code true} as this is a mutable aspect definition
     */
    @Override
    public boolean canRemoveProperties()
    {
        return true;
    }

    /**
     * Adds a property definition to this mutable aspect definition.
     * 
     * @param prop the property definition to add
     * @return the previous property definition with the same name, or {@code null} if none existed
     */
    public PropertyDef add(@NotNull PropertyDef prop)
    {
        return propertyDefs.put(prop.name(), prop);
    }

    /**
     * Removes a property definition from this mutable aspect definition.
     * 
     * @param prop the property definition to remove
     * @return the removed property definition, or {@code null} if it wasn't present
     */
    public PropertyDef remove(@NotNull PropertyDef prop)
    {
        return propertyDefs.remove(prop.name());
    }

}
