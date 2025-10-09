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

import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Basic immutable implementation of a Property in the Cheap system.
 * This implementation stores a property definition and its associated value.
 * <p>
 * Properties are immutable once created - to change a property value,
 * a new PropertyImpl instance must be created.
 * 
 * @see Property
 * @see PropertyDef
 */
public final class PropertyImpl implements Property
{
    /** The property definition describing this property's type and constraints. */
    private final PropertyDef def;
    
    /** The value stored in this property. */
    private final Object val;

    /**
     * Creates a new PropertyImpl with the specified definition and null value.
     * Package-private constructor for internal use.
     * 
     * @param def the property definition for this property
     */
    PropertyImpl(@NotNull PropertyDef def)
    {
        if (!def.hasDefaultValue()) {
            throw new IllegalArgumentException("A value must be provided for Property " + def.name() + " because it has no default value.");
        }
        this.def = def;
        this.val = def.defaultValue();
    }

    /**
     * Creates a new PropertyImpl with the specified definition and value.
     * 
     * @param def the property definition for this property
     * @param val the value to store in this property
     */
    public PropertyImpl(@NotNull PropertyDef def, Object val)
    {
        this.def = def;
        this.val = val;
    }

    /**
     * Returns the property definition for this property.
     * 
     * @return the property definition describing this property's type and constraints
     */
    @Override
    public final PropertyDef def()
    {
        return def;
    }

    /**
     * Returns the raw value stored in this property without type checking.
     * 
     * @return the property value, which may be null
     */
    @Contract(pure = true)
    @Override
    public Object unsafeRead()
    {
        return val;
    }

}
