package net.netbeing.cheap.json.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.netbeing.cheap.impl.basic.EntityImpl;
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

        Map<String, Entity> entityIds = new HashMap<>();

        EntityDirectoryHierarchy hierarchy = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> {
                    HierarchyDef def = context.readValue(p, HierarchyDef.class);
                    hierarchy = factory.createEntityDirectoryHierarchy(def);
                    if (!entityIds.isEmpty()) {
                        hierarchy.putAll(entityIds);
                        entityIds = hierarchy;
                    }
                }
                case "entities" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            String key = p.currentName();
                            p.nextToken();
                            UUID id = UUID.fromString(p.getValueAsString());
                            entityIds.put(key, factory.createEntity(id));
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