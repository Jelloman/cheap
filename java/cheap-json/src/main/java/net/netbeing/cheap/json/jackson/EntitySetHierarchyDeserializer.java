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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class EntitySetHierarchyDeserializer extends JsonDeserializer<EntitySetHierarchy>
{
    private final CheapFactory factory;

    public EntitySetHierarchyDeserializer()
    {
        this(new CheapFactory());
    }

    public EntitySetHierarchyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public EntitySetHierarchy deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        Set<UUID> entityIds = new HashSet<>();

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
                                if (!"entity_set".equals(typeValue)) {
                                    throw new JsonMappingException(p, "Expected type 'entity_set'");
                                }
                            }
                        }
                    }
                }
                case "entities" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            entityIds.add(UUID.fromString(p.getValueAsString()));
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        throw new JsonMappingException(p, "EntitySetHierarchy deserialization requires access to catalog context for proper reconstruction");
    }
}