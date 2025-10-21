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
import net.netbeing.cheap.model.Property;

import java.io.IOException;
import java.util.Collection;

/**
 * Jackson serializer for {@link Property} objects in the Cheap data model.
 * <p>
 * This serializer converts a Property to JSON format, including its definition
 * and value.
 * </p>
 * <p>
 * The serialized format includes:
 * <ul>
 *   <li>def: The PropertyDef that defines this property's type and constraints</li>
 *   <li>value: The actual value of the property (may be null)</li>
 * </ul>
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonSerializer}
 * when serializing standalone Property objects.
 * </p>
 *
 * @see Property
 */
class PropertySerializer extends JsonSerializer<Property>
{
    @Override
    public void serialize(Property property, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();

        // Write the property definition
        gen.writeFieldName("def");
        serializers.findValueSerializer(property.def().getClass()).serialize(property.def(), gen, serializers);

        // Write the value
        gen.writeFieldName("value");
        writeValue(property.unsafeRead(), gen);

        gen.writeEndObject();
    }

    private void writeValue(Object value, JsonGenerator gen) throws IOException
    {
        if (value == null) {
            gen.writeNull();
        } else if (value instanceof String s) {
            gen.writeString(s);
        } else if (value instanceof Number) {
            gen.writeNumber(value.toString());
        } else if (value instanceof Boolean b) {
            gen.writeBoolean(b);
        } else if (value instanceof Collection<?> collection) {
            gen.writeStartArray();
            for (Object item : collection) {
                writeValue(item, gen);
            }
            gen.writeEndArray();
        } else if (value.getClass().isArray()) {
            writeValue(value, gen);
        } else {
            gen.writeString(value.toString());
        }
    }
}
