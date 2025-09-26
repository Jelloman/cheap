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
import java.util.UUID;

class AspectMapHierarchyDeserializer extends JsonDeserializer<AspectMapHierarchy>
{
    private final CheapFactory factory;

    public AspectMapHierarchyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public AspectMapHierarchy deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        AspectDef aspectDef = null;
        JsonNode aspectsData = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> p.skipChildren(); // Skip embedded def (handled by CatalogDeserializer)
                case "aspectDefName" -> {
                    aspectDef = factory.getAspectDef(p.getValueAsString());
                    if (aspectDef == null) {
                        throw new JsonMappingException(p, "AspectDef named '"+p.getValueAsString()+"' not found.");
                    }
                }
                case "aspects" -> {
                    // Store aspects data for later processing
                    aspectsData = context.readTree(p);
                }
                default -> p.skipChildren();
            }
        }

        // Get the current hierarchy name from Jackson context (set by CatalogDeserializer)
        String hierarchyName = (String) context.getAttribute("hierarchyName");
        if (hierarchyName == null) {
            throw new JsonMappingException(p, "No hierarchy name provided in context");
        }

        HierarchyDef def = factory.getHierarchyDef(hierarchyName);
        if (def == null) {
            throw new JsonMappingException(p, "No HierarchyDef found for hierarchy: " + hierarchyName);
        }

        if (aspectDef == null) {
            throw new JsonMappingException(p, "Missing required field: aspectDefName");
        }

        AspectMapHierarchy hierarchy = factory.createAspectMapHierarchy(def, aspectDef);

        // Process aspects data if present
        if (aspectsData != null) {
            JsonParser aspectsParser = aspectsData.traverse(p.getCodec());
            aspectsParser.nextToken();
            deserializeAspects(hierarchy, aspectsParser, context);
        }

        return hierarchy;
    }

    private void deserializeAspects(AspectMapHierarchy hierarchy, JsonParser p, DeserializationContext context)
        throws IOException
    {
        context.setAttribute("CheapAspectDef", hierarchy.aspectDef());
        if (p.currentToken() == JsonToken.START_OBJECT) {
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String entityIdStr = p.currentName();
                UUID entityId = UUID.fromString(entityIdStr);
                Entity key = factory.getOrRegisterNewEntity(entityId);
                context.setAttribute("CheapEntity", key);
                p.nextToken();
                Aspect aspect = context.readValue(p, Aspect.class);
                context.setAttribute("CheapEntity", null);
                hierarchy.put(key, aspect);
            }
        }
        context.setAttribute("CheapAspectDef", null);
    }
}