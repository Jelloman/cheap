package net.netbeing.cheap.json.jackson.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.util.TokenBuffer;
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
        AspectMapHierarchy hierarchy = null;
        HierarchyDef def = null;
        TokenBuffer aspects = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> {
                    def = context.readValue(p, HierarchyDef.class);
                    if (hierarchy == null && aspectDef != null) {
                        hierarchy = factory.createAspectMapHierarchy(def, aspectDef);
                    }
                }
                case "aspectDefName" -> {
                    aspectDef = factory.getAspectDef(p.getValueAsString());
                    if (aspectDef == null) {
                        throw new JsonMappingException(p, "AspectDef named '"+p.getValueAsString()+"' not found.");
                    }
                    if (hierarchy == null && def != null) {
                        hierarchy = factory.createAspectMapHierarchy(def, aspectDef);
                    }
                }
                case "aspects" -> {
                    if (hierarchy == null) {
                        aspects = context.readValue(p, TokenBuffer.class);
                    } else {
                        deserializeAspects(hierarchy, p, context);
                    }
                }
                default -> p.skipChildren();
            }
        }

        if (aspectDef == null) {
            throw new JsonMappingException(p, "Missing required field: aspectDefName");
        }
        if (def == null) {
            throw new JsonMappingException(p, "Missing required field: aspectDefName");
        }
        if (aspects != null) {
            // We encountered the aspects before the AspectDef, so deserialize them now.
            deserializeAspects(hierarchy, aspects.asParser(p), context);
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