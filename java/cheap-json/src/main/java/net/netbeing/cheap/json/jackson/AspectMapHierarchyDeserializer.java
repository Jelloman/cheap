package net.netbeing.cheap.json.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

class AspectMapHierarchyDeserializer extends JsonDeserializer<AspectMapHierarchy>
{
    private final CheapFactory factory;

    public AspectMapHierarchyDeserializer()
    {
        this(new CheapFactory());
    }

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

        String aspectDefName = null;
        AspectMapHierarchy hierarchy = null;
        HierarchyDef def = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> {
                    def = context.readValue(p, HierarchyDef.class);
                    // We need an AspectDef to create the hierarchy, we'll get it from aspectDefName
                    // For now, create the hierarchy without the AspectDef and set it later
                }
                case "aspectDefName" -> {
                    aspectDefName = p.getValueAsString();
                    if (hierarchy == null) {
                        // Create a minimal AspectDef for this hierarchy
                        AspectDef aspectDef = factory.createMutableAspectDef(aspectDefName);
                        hierarchy = factory.createAspectMapHierarchy(aspectDef);
                    }
                }
                case "aspects" -> p.skipChildren(); // Skip aspects - they'll be populated separately
                default -> p.skipChildren();
            }
        }

        if (hierarchy == null) {
            if (aspectDefName != null) {
                AspectDef aspectDef = factory.createMutableAspectDef(aspectDefName);
                hierarchy = factory.createAspectMapHierarchy(aspectDef);
            } else {
                throw new JsonMappingException(p, "Missing required field: aspectDefName");
            }
        }

        return hierarchy;
    }
}