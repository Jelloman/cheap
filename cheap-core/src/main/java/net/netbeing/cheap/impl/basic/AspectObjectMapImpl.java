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

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Basic implementation of an Aspect that stores properties in a HashMap.
 * Not all properties must be explicitly assigned values; any property included
 * in the AspectDef that is not explicitly assigned.
 *
 * @see AspectBaseImpl
 * @see Aspect
 */
public class AspectObjectMapImpl extends AspectBaseImpl
{
    /** Internal map storing property names to property objects. */
    protected Map<String, Object> props;

    /**
     * Creates a new AspectObjectMapImpl with default capacity.
     * 
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     */
    public AspectObjectMapImpl(Entity entity, @NotNull AspectDef def)
    {
        super(entity, def);
        this.props = new LinkedHashMap<>();
    }

    /**
     * Creates a new AspectObjectMapImpl with specified initial capacity.
     * 
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity of the internal map
     */
    public AspectObjectMapImpl(Entity entity, @NotNull AspectDef def, int initialCapacity)
    {
        super(entity, def);
        this.props = LinkedHashMap.newLinkedHashMap(initialCapacity);
    }

    /**
     * Creates a new AspectObjectMapImpl with specified initial capacity and load factor.
     * 
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity of the internal map
     * @param loadFactor the load factor of the internal map
     */
    public AspectObjectMapImpl(Entity entity, @NotNull AspectDef def, int initialCapacity, float loadFactor)
    {
        super(entity, def);
        this.props = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Checks if this aspect contains a property with the given name.
     * 
     * @param propName the name of the property to check for
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    @Override
    public boolean contains(@NotNull String propName)
    {
        if (props.containsKey(propName)) {
            return true;
        }
        PropertyDef propDef = def.propertyDef(propName);
        return propDef != null && propDef.hasDefaultValue();
    }

    /**
     * Reads a property value without type safety checks.
     * 
     * @param propName the name of the property to read
     * @return the property object, or {@code null} if not found
     */
    @Override
    public Object unsafeReadObj(@NotNull String propName)
    {
        if (props.containsKey(propName)) {
            return props.get(propName);
        }
        PropertyDef propDef = def.propertyDef(propName);
        return (propDef != null && propDef.hasDefaultValue()) ? propDef.defaultValue() : null;
    }

    /**
     * Adds a property to this aspect without validation.
     * 
     * @param prop the property to add
     */
    @Override
    public void unsafeAdd(@NotNull Property prop)
    {
        props.put(prop.def().name(), prop.unsafeRead());
    }

    /**
     * Writes a property value without type safety checks.
     * 
     * @param propName the name of the property to write
     * @param value the value to write
     * @throws IllegalArgumentException if the property name is not defined in this aspect
     */
    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        AspectDef def = def();
        PropertyDef stdPropDef = def.propertyDef(propName);
        if (stdPropDef == null) {
            throw new IllegalArgumentException("Aspect '" + def().name() + "' does not contain prop named '" + propName + "'");
        }
        props.put(propName, value);
    }

    /**
     * Removes a property from this aspect without validation.
     * 
     * @param propName the name of the property to remove
     */
    @Override
    public void unsafeRemove(@NotNull String propName)
    {
        props.remove(propName);
    }
}
