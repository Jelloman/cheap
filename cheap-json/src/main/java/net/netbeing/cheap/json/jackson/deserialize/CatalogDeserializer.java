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
import com.fasterxml.jackson.databind.JsonNode;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Jackson deserializer for {@link Catalog} objects in the Cheap data model.
 * <p>
 * This deserializer reconstructs a Catalog from JSON format, creating all necessary
 * AspectDef and Hierarchy objects and populating them with data from the JSON structure.
 * The deserializer uses a {@link CheapFactory} to create model objects consistently.
 * </p>
 * <p>
 * The deserialization process follows a specific order:
 * </p>
 * <ol>
 *   <li>Parse catalog metadata (globalId, URI, species, upstream, version)</li>
 *   <li>Deserialize and register all AspectDefs with the factory</li>
 *   <li>Store hierarchy JSON data for deferred processing</li>
 *   <li>Create the Catalog instance</li>
 *   <li>Deserialize hierarchies (which can now resolve AspectDef references)</li>
 * </ol>
 * <p>
 * This two-phase approach ensures that AspectDef objects are available when deserializing
 * AspectMapHierarchy contents, avoiding forward reference issues.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonDeserializer}.
 * </p>
 *
 * @see Catalog
 * @see CheapJacksonDeserializer
 * @see CheapFactory
 */
class CatalogDeserializer extends JsonDeserializer<Catalog>
{
    private final CheapFactory factory;

    public CatalogDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public Catalog deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        UUID globalId = null;
        URI uri = null;
        CatalogSpecies species = null;
        UUID upstreamId = null;
        long version = 0L;
        List<JsonNode> hierarchyData = new LinkedList<>();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "globalId" -> globalId = UUID.fromString(p.getValueAsString());
                case "uri" -> {
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        uri = URI.create(p.getValueAsString());
                    }
                }
                case "species" -> species = CatalogSpecies.valueOf(p.getValueAsString().toUpperCase());
                case "version" -> version = p.getLongValue();
                case "upstream" -> {
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        upstreamId = UUID.fromString(p.getValueAsString());
                    }
                }
                case "aspectDefs" -> {
                    // Register all aspect defs with the factory
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            //String aspectDefName = p.currentName();
                            p.nextToken();
                            AspectDef aspectDef = p.readValueAs(AspectDef.class);
                            factory.registerAspectDef(aspectDef);
                        }
                    }
                }
                case "hierarchies" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            //String hierarchyName = p.currentName();
                            p.nextToken();
                            // Store the raw JSON structure for later processing
                            hierarchyData.add(context.readTree(p));
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        if (globalId == null || species == null) {
            throw new JsonMappingException(p, "Missing required fields: globalId and species");
        }

        // Create the catalog first
        Catalog catalog = factory.createCatalog(globalId, species, uri, upstreamId, version);
        factory.setCatalog(catalog);

        // Now deserialize and add hierarchies
        for (JsonNode node : hierarchyData) {
            // Create a new parser for this hierarchy's data
            JsonParser hierarchyParser = node.traverse(p.getCodec());
            hierarchyParser.nextToken();

            Hierarchy hierarchy = context.readValue(hierarchyParser, Hierarchy.class);
            if (catalog.hierarchy(hierarchy.name()) != hierarchy) {
                throw new JsonMappingException(p, "Error adding deserialized hierarchy '"+hierarchy.name()+"' to catalog.");
            }
        }

        return catalog;
    }
}