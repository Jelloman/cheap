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
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;

import java.io.IOException;

/**
 * Jackson serializer for {@link HierarchyDef} objects in the Cheap data model.
 * <p>
 * This serializer converts a HierarchyDef to JSON format, including its name
 * and type code. HierarchyDef is a simple structure that specifies the metadata
 * for a hierarchy, defining its name and organizational type (EL, ES, ED, ET, or AM).
 * </p>
 * <p>
 * The serializer produces compact JSON with just two fields:
 * </p>
 * <ul>
 *   <li>name: The unique name of the hierarchy within its catalog</li>
 *   <li>type: The two-character type code (EL, ES, ED, ET, or AM)</li>
 * </ul>
 * <p>
 * This class is package-private and used internally by {@link CatalogDefSerializer}
 * when serializing catalog definitions, or by {@link CheapJacksonSerializer} for
 * standalone hierarchy definition serialization.
 * </p>
 *
 * @see HierarchyDef
 * @see HierarchyType
 * @see CatalogDefSerializer
 */
class HierarchyDefSerializer extends JsonSerializer<HierarchyDef>
{
    @Override
    public void serialize(HierarchyDef hierarchyDef, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeStringField("name", hierarchyDef.name());
        gen.writeStringField("type", hierarchyDef.type().typeCode());        
        gen.writeEndObject();
    }
}