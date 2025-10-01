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
import java.util.*;

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
            catalog.addHierarchy(hierarchy);
        }

        return catalog;
    }
}