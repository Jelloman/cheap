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
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectMap;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityDirectoryHierarchy;
import net.netbeing.cheap.model.EntityListHierarchy;
import net.netbeing.cheap.model.EntitySetHierarchy;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyType;

import java.io.IOException;
import java.util.Map;

/**
 * Jackson serializer for {@link AspectMap} objects in the Cheap data model.
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonSerializer}
 * when serializing Catalog objects that contain Hierarchy collections.
 * </p>
 *
 * @see AspectMap
 */
class AspectMapSerializer extends JsonSerializer<AspectMap>
{
    @Override
    public void serialize(AspectMap aspectMap, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();

        gen.writeStringField("aspectDef", aspectMap.aspectDef().name());

        gen.writeFieldName("contents");

        gen.writeStartObject();
        for (Map.Entry<Entity, Aspect> entry : aspectMap.entrySet()) {
            gen.writeFieldName(entry.getKey().globalId().toString());
            gen.writeObject(entry.getValue());
        }
        gen.writeEndObject();

        gen.writeEndObject();
    }

}