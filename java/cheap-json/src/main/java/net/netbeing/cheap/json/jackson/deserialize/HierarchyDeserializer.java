package net.netbeing.cheap.json.jackson.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

class HierarchyDeserializer extends JsonDeserializer<Hierarchy>
{
    private final CheapFactory factory;

    public HierarchyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public Hierarchy deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        HierarchyType type = null;
        String name = null;

        // Need to peek ahead to determine type
        while (p.nextToken() != JsonToken.END_OBJECT) {
            switch (p.currentName()) {
                case "type" -> {
                    try {
                        type = HierarchyType.fromTypeCode(p.getValueAsString().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new JsonMappingException(p, "", e);
                    }
                }
                case "name" -> name = p.getValueAsString();
                case "content" -> {
                    if (type == null || name == null) {
                        throw new JsonMappingException(p, "Expected type and name as first two fields of hierarchy element.");
                    }
                    p.nextToken();
                    switch (type) {
                        case ASPECT_MAP -> { return readAspectMapHierarchy(name, p, context); }
                        case ENTITY_DIR -> { return readEntityDirectoryHierarchy(name, p); }
                        case ENTITY_LIST -> { return readEntityListHierarchy(name, p); }
                        case ENTITY_SET -> { return readEntitySetHierarchy(name, p); }
                        case ENTITY_TREE -> { return readEntityTreeHierarchy(name, p); }
                        default -> throw new JsonMappingException(p, "Unknown hierarchy type: " + type);
                    }

                }
            }
        }
        throw new JsonMappingException(p, "Badly formed hierarchy JSON.");
    }

    AspectMapHierarchy readAspectMapHierarchy(String name, JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        AspectDef aspectDef = factory.getAspectDef(name);
        if (aspectDef == null) {
            throw new JsonMappingException(p, "AspectDef named '" + name + "' not found.");
        }
        AspectMapHierarchy hierarchy = factory.createAspectMapHierarchy(name, aspectDef);

        context.setAttribute("CheapAspectDef", aspectDef);
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
        context.setAttribute("CheapAspectDef", null);

        return hierarchy;
    }

    EntityDirectoryHierarchy readEntityDirectoryHierarchy(String name, JsonParser p) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }
        EntityDirectoryHierarchy hierarchy = factory.createEntityDirectoryHierarchy(name);
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            p.nextToken();
            UUID id = UUID.fromString(p.getValueAsString());
            hierarchy.put(key, factory.getOrRegisterNewEntity(id));
        }
        return hierarchy;
    }

    EntityListHierarchy readEntityListHierarchy(String name, JsonParser p) throws IOException
    {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw new JsonMappingException(p, "Expected START_ARRAY token");
        }
        EntityListHierarchy hierarchy = factory.createEntityListHierarchy(name);
        while (p.nextToken() != JsonToken.END_ARRAY) {
            UUID id = UUID.fromString(p.getValueAsString());
            hierarchy.add(factory.getOrRegisterNewEntity(id));
        }
        return hierarchy;
    }

    EntitySetHierarchy readEntitySetHierarchy(String name, JsonParser p) throws IOException
    {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw new JsonMappingException(p, "Expected START_ARRAY token");
        }
        EntitySetHierarchy hierarchy = factory.createEntitySetHierarchy(name);
        while (p.nextToken() != JsonToken.END_ARRAY) {
            UUID id = UUID.fromString(p.getValueAsString());
            hierarchy.add(factory.getOrRegisterNewEntity(id));
        }
        return hierarchy;
    }

    EntityTreeHierarchy readEntityTreeHierarchy(String name, JsonParser p) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }
        EntityTreeHierarchy.Node root = p.readValueAs(EntityTreeHierarchy.Node.class);
        return factory.createEntityTreeHierarchy(name, root);
    }
}