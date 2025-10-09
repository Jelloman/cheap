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
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson deserializer for {@link CatalogDef} objects in the Cheap data model.
 * <p>
 * This deserializer reconstructs a CatalogDef from JSON format, including its collections
 * of AspectDef and HierarchyDef objects. A CatalogDef provides informational metadata
 * describing the structure and types that a catalog contains or typically contains.
 * </p>
 * <p>
 * The deserialization process delegates to {@link AspectDefDeserializer} and
 * {@link HierarchyDefDeserializer} for nested objects. AspectDef registration is
 * handled by AspectDefDeserializer, while HierarchyDefs are registered with the
 * factory by this deserializer.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonDeserializer}
 * for standalone CatalogDef deserialization or as part of metadata imports.
 * </p>
 *
 * @see CatalogDef
 * @see AspectDef
 * @see HierarchyDef
 * @see CheapFactory
 * @see AspectDefDeserializer
 * @see HierarchyDefDeserializer
 */
class CatalogDefDeserializer extends JsonDeserializer<CatalogDef>
{
    private final CheapFactory factory;

    public CatalogDefDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public CatalogDef deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        List<AspectDef> aspectDefs = new ArrayList<>();
        List<HierarchyDef> hierarchyDefs = new ArrayList<>();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "aspectDefs" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            p.nextToken(); // Move to value
                            AspectDef aspectDef = p.readValueAs(AspectDef.class);
                            aspectDefs.add(aspectDef);
                            // AspectDef registration is already handled by AspectDefDeserializer
                        }
                    }
                }
                case "hierarchyDefs" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            HierarchyDef hierarchyDef = p.readValueAs(HierarchyDef.class);
                            hierarchyDefs.add(hierarchyDef);
                            factory.registerHierarchyDef(hierarchyDef);
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        return factory.createCatalogDef(hierarchyDefs, aspectDefs);
    }
}