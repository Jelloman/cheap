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
import java.util.HashMap;
import java.util.Map;

class HierarchyDirDeserializer extends JsonDeserializer<HierarchyDir>
{
    private final CheapFactory factory;

    public HierarchyDirDeserializer()
    {
        this(new CheapFactory());
    }

    public HierarchyDirDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public HierarchyDir deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        Map<String, String> hierarchyNames = new HashMap<>();

        HierarchyDir hierarchy = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> {
                    HierarchyDef def = context.readValue(p, HierarchyDef.class);
                    hierarchy = factory.createHierarchyDir(def);
                }
                case "hierarchies" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            String key = p.currentName();
                            p.nextToken();
                            hierarchyNames.put(key, p.getValueAsString());
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        if (hierarchy == null) {
            throw new JsonMappingException(p, "Missing required field: def");
        }

        return hierarchy;
    }
}