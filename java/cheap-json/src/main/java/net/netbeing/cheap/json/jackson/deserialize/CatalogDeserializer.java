package net.netbeing.cheap.json.jackson.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        boolean strict = false;
        CatalogDef def = null;
        UUID upstreamId = null;
        Map<String, JsonNode> hierarchyData = new HashMap<>();

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
                case "strict" -> strict = p.getBooleanValue();
                case "def" -> def = p.readValueAs(CatalogDef.class);
                case "upstream" -> {
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        upstreamId = UUID.fromString(p.getValueAsString());
                    }
                }
                case "aspectDefs" -> p.skipChildren(); // Already handled in def
                case "hierarchies" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            String hierarchyName = p.currentName();
                            p.nextToken();
                            // Store the raw JSON structure for later processing
                            hierarchyData.put(hierarchyName, context.readTree(p));
                        }
                    }
                }
                case "aspects" -> p.skipChildren();
                default -> p.skipChildren();
            }
        }

        if (globalId == null || species == null || def == null) {
            throw new JsonMappingException(p, "Missing required fields: globalId, species, and def");
        }

        // Create the catalog first
        Catalog catalog = factory.createCatalog(globalId, species, def, upstreamId, strict);

        // Now deserialize and add hierarchies using the registered HierarchyDefs
        for (Map.Entry<String, JsonNode> entry : hierarchyData.entrySet()) {
            String hierarchyName = entry.getKey();
            HierarchyDef hierarchyDef = factory.getHierarchyDef(hierarchyName);

            if (hierarchyDef == null) {
                throw new JsonMappingException(p, "No HierarchyDef found for hierarchy: " + hierarchyName);
            }

            // Create a new parser for this hierarchy's data
            JsonParser hierarchyParser = entry.getValue().traverse(p.getCodec());
            hierarchyParser.nextToken();

            // Set the hierarchy name in context for the deserializers
            context.setAttribute("hierarchyName", hierarchyName);

            Hierarchy hierarchy = switch (hierarchyDef.type()) {
                case ASPECT_MAP -> context.readValue(hierarchyParser, AspectMapHierarchy.class);
                case ENTITY_DIR -> context.readValue(hierarchyParser, EntityDirectoryHierarchy.class);
                case ENTITY_LIST -> context.readValue(hierarchyParser, EntityListHierarchy.class);
                case ENTITY_SET -> context.readValue(hierarchyParser, EntitySetHierarchy.class);
                case ENTITY_TREE -> context.readValue(hierarchyParser, EntityTreeHierarchy.class);
            };

            catalog.addHierarchy(hierarchy);
        }

        return catalog;
    }
}