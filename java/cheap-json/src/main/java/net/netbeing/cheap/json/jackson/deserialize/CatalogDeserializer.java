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
        UUID upstreamId = null;
        long version = 0L;
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
                            String aspectDefName = p.currentName();
                            p.nextToken();
                            AspectDef aspectDef = p.readValueAs(AspectDef.class);
                            factory.registerAspectDef(aspectDef);
                        }
                    }
                }
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
                default -> p.skipChildren();
            }
        }

        if (globalId == null || species == null) {
            throw new JsonMappingException(p, "Missing required fields: globalId and species");
        }

        // Create the catalog first
        Catalog catalog = factory.createCatalog(globalId, species, upstreamId, version);
        factory.setCatalog(catalog);

        // Now deserialize and add hierarchies
        for (Map.Entry<String, JsonNode> entry : hierarchyData.entrySet()) {
            String hierarchyName = entry.getKey();
            JsonNode hierarchyJson = entry.getValue();

            // Create a new parser for this hierarchy's data
            JsonParser hierarchyParser = entry.getValue().traverse(p.getCodec());
            hierarchyParser.nextToken();

            // Set the hierarchy name in context for the deserializers
            context.setAttribute("hierarchyName", hierarchyName);

            // The hierarchy deserializers will determine the type from the JSON
            // We'll need to check the "def" field to determine the type
            if (hierarchyJson.has("def") && hierarchyJson.get("def").has("type")) {
                String typeCode = hierarchyJson.get("def").get("type").asText().toUpperCase();
                HierarchyType hierarchyType = switch (typeCode) {
                    case "EL" -> HierarchyType.ENTITY_LIST;
                    case "ES" -> HierarchyType.ENTITY_SET;
                    case "ED" -> HierarchyType.ENTITY_DIR;
                    case "ET" -> HierarchyType.ENTITY_TREE;
                    case "AM" -> HierarchyType.ASPECT_MAP;
                    default -> throw new JsonMappingException(p, "Unknown hierarchy type: " + typeCode);
                };

                Hierarchy hierarchy = switch (hierarchyType) {
                    case ASPECT_MAP -> context.readValue(hierarchyParser, AspectMapHierarchy.class);
                    case ENTITY_DIR -> context.readValue(hierarchyParser, EntityDirectoryHierarchy.class);
                    case ENTITY_LIST -> context.readValue(hierarchyParser, EntityListHierarchy.class);
                    case ENTITY_SET -> context.readValue(hierarchyParser, EntitySetHierarchy.class);
                    case ENTITY_TREE -> context.readValue(hierarchyParser, EntityTreeHierarchy.class);
                };
            } else {
                throw new JsonMappingException(p, "Hierarchy missing type information: " + hierarchyName);
            }
        }

        return catalog;
    }
}