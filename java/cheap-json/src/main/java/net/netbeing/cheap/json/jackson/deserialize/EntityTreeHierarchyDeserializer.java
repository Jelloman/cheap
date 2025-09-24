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
        EntityTreeHierarchy hierarchy = null;
        HierarchyDef def = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> {
                    def = context.readValue(p, HierarchyDef.class);
                    if (root != null) {
                        hierarchy = factory.createEntityTreeHierarchy(def, root);
                    }
                }
                case "root" -> {
                    root = p.readValueAs(EntityTreeHierarchy.Node.class);
                    if (def != null) {
                        hierarchy = factory.createEntityTreeHierarchy(def, root);
                    }
                }
                default -> p.skipChildren();
            }
        }

        if (hierarchy == null) {
            throw new JsonMappingException(p, "Missing required fields: def and root");
        }

        return hierarchy;
    }
}