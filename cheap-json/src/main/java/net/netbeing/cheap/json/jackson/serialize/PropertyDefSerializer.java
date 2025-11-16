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

package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.PropertyDef;

import java.io.IOException;

/**
 * Jackson serializer for {@link PropertyDef} objects in the Cheap data model.
 * <p>
 * This serializer converts a PropertyDef to JSON format, including its name, type,
 * and various configuration flags.
 * </p>
 * <p>
 * The serializer optimizes the JSON output by omitting boolean flags that have
 * their default values to reduce JSON verbosity while maintaining full fidelity.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonSerializer}
 * when serializing standalone PropertyDef objects.
 * </p>
 *
 * @see PropertyDef
 */
class PropertyDefSerializer extends JsonSerializer<PropertyDef>
{
    @Override
    public void serialize(PropertyDef propertyDef, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();

        gen.writeStringField("name", propertyDef.name());
        gen.writeStringField("type", propertyDef.type().name());

        // Only write non-default values for all the following fields

        if (propertyDef.hasDefaultValue()) {
            gen.writeFieldName("defaultValue");
            writeValue(propertyDef.defaultValue(), gen);
        }

        if (!propertyDef.isReadable()) {
            gen.writeBooleanField("isReadable", false);
        }
        if (!propertyDef.isWritable()) {
            gen.writeBooleanField("isWritable", false);
        }
        if (!propertyDef.isNullable()) {
            gen.writeBooleanField("isNullable", false);
        }
        if (propertyDef.isMultivalued()) {
            gen.writeBooleanField("isMultivalued", true);
        }

        gen.writeEndObject();
    }

    private void writeValue(Object value, JsonGenerator gen) throws IOException
    {
        switch (value) {
            case null -> gen.writeNull();
            case String s -> gen.writeString(s);
            case Number _ -> gen.writeNumber(value.toString());
            case Boolean b -> gen.writeBoolean(b);
            default -> gen.writeString(value.toString());
        }
    }
}
