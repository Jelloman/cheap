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

class AspectDefDirHierarchyDeserializer extends JsonDeserializer<AspectDefDirHierarchy>
{
    private final CheapFactory factory;

    public AspectDefDirHierarchyDeserializer()
    {
        this(new CheapFactory());
    }

    public AspectDefDirHierarchyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public AspectDefDirHierarchy deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        AspectDefDirHierarchy hierarchy = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> {
                    HierarchyDef def = context.readValue(p, HierarchyDef.class);
                    hierarchy = factory.createAspectDefDirHierarchy(def);
                }
                case "aspectDefs" -> p.skipChildren(); // Skip aspect defs - they'll be populated separately
                default -> p.skipChildren();
            }
        }

        if (hierarchy == null) {
            throw new JsonMappingException(p, "Missing required field: def");
        }

        return hierarchy;
    }
}