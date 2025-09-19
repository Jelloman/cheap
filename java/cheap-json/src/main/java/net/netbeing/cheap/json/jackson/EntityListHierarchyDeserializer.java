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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class EntityListHierarchyDeserializer extends JsonDeserializer<EntityListHierarchy>
{
    private final CheapFactory factory;

    public EntityListHierarchyDeserializer()
    {
        this(new CheapFactory());
    }

    public EntityListHierarchyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public EntityListHierarchy deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        List<Entity> entityIds = new ArrayList<>();

        EntityListHierarchy hierarchy = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> {
                    HierarchyDef def = context.readValue(p, HierarchyDef.class);
                    hierarchy = factory.createEntityListHierarchy(def);
                    if (!entityIds.isEmpty()) {
                        hierarchy.addAll(entityIds);
                        entityIds = hierarchy;
                    }
                }
                case "entities" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            UUID id = UUID.fromString(p.getValueAsString());
                            entityIds.add(factory.createEntity(id));
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