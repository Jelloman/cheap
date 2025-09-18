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
import java.util.UUID;

class TreeNodeDeserializer extends JsonDeserializer<EntityTreeHierarchy.Node>
{
    private final CheapFactory factory;

    public TreeNodeDeserializer()
    {
        this(new CheapFactory());
    }

    public TreeNodeDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public EntityTreeHierarchy.Node deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        UUID entityId = null;
        Map<String, EntityTreeHierarchy.Node> children = new HashMap<>();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "entityId" -> {
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        entityId = UUID.fromString(p.getValueAsString());
                    }
                }
                case "children" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            String key = p.currentName();
                            p.nextToken();
                            children.put(key, p.readValueAs(EntityTreeHierarchy.Node.class));
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        throw new JsonMappingException(p, "TreeNode deserialization requires access to hierarchy context for proper reconstruction");
    }
}