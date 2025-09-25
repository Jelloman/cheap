package net.netbeing.cheap.json.jackson.deserialize;

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

        List<Entity> entities = new ArrayList<>();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "entities" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            UUID id = UUID.fromString(p.getValueAsString());
                            entities.add(factory.getOrRegisterNewEntity(id));
                        }
                    }
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

        EntityListHierarchy hierarchy = factory.createEntityListHierarchy(def);
        hierarchy.addAll(entities);

        return hierarchy;
    }
}