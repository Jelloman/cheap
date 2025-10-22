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

package net.netbeing.cheap.json.jackson.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Jackson deserializer for {@link PropertyDef} objects in the Cheap data model.
 * <p>
 * This deserializer reconstructs a PropertyDef from JSON format, including its name,
 * type, default value (if any), and all configuration flags (isReadable, isWritable,
 * isNullable, isRemovable, isMultivalued).
 * </p>
 * <p>
 * PropertyDef objects are typically deserialized as part of an AspectDef, where they
 * are embedded inline within the "propertyDefs" array. However, this deserializer can
 * also handle standalone PropertyDef deserialization if needed.
 * </p>
 * <p>
 * The deserializer uses the {@link CheapFactory#createPropertyDef} method to ensure
 * consistent PropertyDef creation with proper defaults and validation.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link AspectDefDeserializer}
 * when deserializing AspectDef property collections.
 * </p>
 *
 * @see PropertyDef
 * @see AspectDefDeserializer
 * @see CheapFactory
 */
class PropertyDefDeserializer extends JsonDeserializer<PropertyDef>
{
    private final CheapFactory factory;

    public PropertyDefDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public PropertyDef deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        String name = null;
        PropertyType type = null;
        Object defaultValue = null;
        boolean hasDefaultValue = false;
        boolean isReadable = true;
        boolean isWritable = true;
        boolean isNullable = false;
        boolean isRemovable = false;
        boolean isMultivalued = false;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "name" -> name = p.getValueAsString();
                case "type" -> type = PropertyType.valueOf(p.getValueAsString());
                case "hasDefaultValue" -> hasDefaultValue = p.getBooleanValue();
                case "defaultValue" -> defaultValue = readValue(p, type);
                case "isReadable" -> isReadable = p.getBooleanValue();
                case "isWritable" -> isWritable = p.getBooleanValue();
                case "isNullable" -> isNullable = p.getBooleanValue();
                case "isRemovable" -> isRemovable = p.getBooleanValue();
                case "isMultivalued" -> isMultivalued = p.getBooleanValue();
                default -> p.skipChildren(); // Skip unknown fields
            }
        }

        if (name == null || type == null) {
            throw new JsonMappingException(p, "Missing required fields: name and type");
        }

        return factory.createPropertyDef(name, type, defaultValue, hasDefaultValue,
                                       isReadable, isWritable, isNullable, isRemovable, isMultivalued);
    }

    private Object readValue(JsonParser p, PropertyType type) throws IOException
    {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        if (type == null) {
            return p.getValueAsString();
        }

        return switch (type) {
            case Integer -> p.getLongValue();
            case Float -> p.getDoubleValue();
            case Boolean -> p.getBooleanValue();
            default -> p.getValueAsString();
        };
    }
}