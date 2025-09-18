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

class EntityDirectoryHierarchyDeserializer extends JsonDeserializer<EntityDirectoryHierarchy>
{
    private final CheapFactory factory;

    public EntityDirectoryHierarchyDeserializer()
    {
        this(new CheapFactory());
    }

    public EntityDirectoryHierarchyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public EntityDirectoryHierarchy deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        Map<String, UUID> entityIds = new HashMap<>();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            String defField = p.currentName();
                            p.nextToken();
                            if ("type".equals(defField)) {
                                String typeValue = p.getValueAsString();
                                if (!"entity_dir".equals(typeValue)) {
                                    throw new JsonMappingException(p, "Expected type 'entity_dir'");
                                }
                            }
                        }
                    }
                }
                case "entities" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            String key = p.currentName();
                            p.nextToken();
                            entityIds.put(key, UUID.fromString(p.getValueAsString()));
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        throw new JsonMappingException(p, "EntityDirectoryHierarchy deserialization requires access to catalog context for proper reconstruction");
    }
}