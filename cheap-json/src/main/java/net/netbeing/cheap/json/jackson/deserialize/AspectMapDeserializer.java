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
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMap;
import net.netbeing.cheap.model.Entity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

/**
 * Jackson deserializer for {@link AspectMap} objects in the Cheap data model.
 * <p>
 * Deserializes entity-to-aspect mappings, using context attributes to pass AspectDef
 * and Entity to the AspectDeserializer
 * <p>
 * Entity references are resolved through the CheapFactory's entity registry, ensuring
 * that entities with the same UUID are represented by the same Entity object instances.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonDeserializer}
 * and {@link CatalogDeserializer}.
 * </p>
 *
 * @see AspectMap
 */
class AspectMapDeserializer extends JsonDeserializer<AspectMap>
{
    private final CheapFactory factory;

    public AspectMapDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public AspectMap deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        AspectMap map = null;

        // Need to peek ahead to determine type
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();

            switch (field) {
                case "aspectDef" -> {
                    String aspectDefName = p.getValueAsString();
                    AspectDef def = factory.getAspectDef(aspectDefName);
                    if (def == null) {
                        throw new JsonMappingException(p, "AspectDef named '"+aspectDefName+"' not registered in CheapFactory.");
                    }
                    map = factory.createAspectMap(def);
                }
                case "contents" -> {
                    if (map == null) {
                        throw new JsonMappingException(p, "Missing AspectDef.");
                    }
                    readContents(map, p, context);
                }
                default -> throw new JsonMappingException(p, "Unrecognized hierarchy field '"+field+"'.");
            }
        }
        return map;
    }

    void readContents(AspectMap map, JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        context.setAttribute("CheapAspectDef", map.aspectDef());
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String entityIdStr = p.currentName();
            UUID entityId = UUID.fromString(entityIdStr);
            Entity key = factory.getOrRegisterNewEntity(entityId);
            context.setAttribute("CheapEntity", key);
            p.nextToken();
            Aspect aspect = context.readValue(p, Aspect.class);
            context.setAttribute("CheapEntity", null);
            map.put(key, aspect);
        }
        context.setAttribute("CheapAspectDef", null);
    }

}