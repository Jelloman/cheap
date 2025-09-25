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

class EntityTreeHierarchyDeserializer extends JsonDeserializer<EntityTreeHierarchy>
{
    private final CheapFactory factory;

    public EntityTreeHierarchyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public EntityTreeHierarchy deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        EntityTreeHierarchy.Node root = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "root" -> {
                    root = p.readValueAs(EntityTreeHierarchy.Node.class);
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

        if (root == null) {
            throw new JsonMappingException(p, "Missing required field: root");
        }

        EntityTreeHierarchy hierarchy = factory.createEntityTreeHierarchy(def, root);

        return hierarchy;
    }
}