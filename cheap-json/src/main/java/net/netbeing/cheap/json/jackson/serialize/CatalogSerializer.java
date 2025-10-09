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
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.util.Map;

/**
 * Jackson serializer for {@link Catalog} objects in the Cheap data model.
 * <p>
 * This serializer converts a Catalog to JSON format, including its global ID, URI,
 * species, upstream catalog reference, aspect definitions, and all contained hierarchies.
 * The output JSON structure provides a complete representation of the catalog suitable
 * for storage or transmission.
 * </p>
 * <p>
 * The serializer produces JSON with the following structure:
 * <pre>{@code
 * {
 *   "globalId": "uuid-string",
 *   "uri": "catalog-uri" or null,
 *   "species": "source|sink|mirror|cache|clone|fork",
 *   "upstream": "upstream-catalog-uuid" (optional),
 *   "aspectDefs": {
 *     "aspectDefName": { AspectDef JSON... },
 *     ...
 *   },
 *   "hierarchies": {
 *     "hierarchyName": { Hierarchy JSON... },
 *     ...
 *   }
 * }
 * }</pre>
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonSerializer}.
 * It delegates to other specialized serializers for nested objects like AspectDef and Hierarchy.
 * </p>
 *
 * @see Catalog
 * @see CheapJacksonSerializer
 */
class CatalogSerializer extends JsonSerializer<Catalog>
{
    @Override
    public void serialize(Catalog catalog, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeStringField("globalId", catalog.globalId().toString());
        
        if (catalog.uri() != null) {
            gen.writeStringField("uri", catalog.uri().toString());
        } else {
            gen.writeNullField("uri");
        }
        
        gen.writeStringField("species", catalog.species().name().toLowerCase());
        if (catalog.upstream() != null) {
            gen.writeStringField("upstream", catalog.upstream().toString());
        }

        gen.writeFieldName("aspectDefs");
        gen.writeStartObject();
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            gen.writeFieldName(aspectDef.name());
            gen.writeObject(aspectDef);
        }
        gen.writeEndObject();

        gen.writeFieldName("hierarchies");
        gen.writeStartObject();
        for (Hierarchy h : catalog.hierarchies()) {
            gen.writeFieldName(h.name());
            gen.writeObject(h);
        }
        gen.writeEndObject();
        
        gen.writeEndObject();
    }
}