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

import net.netbeing.cheap.model.PropertyType;

public class PropertyDefBuilder
{
    private String name;
    private PropertyType type;
    private Object defaultValue = null;
    private boolean hasDefaultValue = false;
    private boolean isReadable = true;
    private boolean isWritable = true;
    private boolean isNullable = true;
    private boolean isMultivalued = false;

    public PropertyDefBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    public PropertyDefBuilder setType(PropertyType type)
    {
        this.type = type;
        return this;
    }

    public PropertyDefBuilder setDefaultValue(Object defaultValue)
    {
        this.defaultValue = defaultValue;
        this.hasDefaultValue = true;
        return this;
    }

    public PropertyDefBuilder setHasDefaultValue(boolean hasDefaultValue)
    {
        this.hasDefaultValue = hasDefaultValue;
        if (!hasDefaultValue) {
            this.defaultValue = null;
        }
        return this;
    }

    public PropertyDefBuilder setIsReadable(boolean isReadable)
    {
        this.isReadable = isReadable;
        return this;
    }

    public PropertyDefBuilder setIsWritable(boolean isWritable)
    {
        this.isWritable = isWritable;
        return this;
    }

    public PropertyDefBuilder setIsNullable(boolean isNullable)
    {
        this.isNullable = isNullable;
        return this;
    }

    public PropertyDefBuilder setIsMultivalued(boolean isMultivalued)
    {
        this.isMultivalued = isMultivalued;
        return this;
    }

    public PropertyDefImpl build()
    {
        return new PropertyDefImpl(name, type, defaultValue, hasDefaultValue, isReadable, isWritable, isNullable, isMultivalued);
    }
}