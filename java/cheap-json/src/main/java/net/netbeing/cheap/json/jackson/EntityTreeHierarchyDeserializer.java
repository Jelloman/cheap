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

class EntityTreeHierarchyDeserializer extends JsonDeserializer<EntityTreeHierarchy>
{
    private final CheapFactory factory;

    public EntityTreeHierarchyDeserializer()
    {
        this(new CheapFactory());
    }

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
                case "def" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            String defField = p.currentName();
                            p.nextToken();
                            if ("type".equals(defField)) {
                                String typeValue = p.getValueAsString();
                                if (!"entity_tree".equals(typeValue)) {
                                    throw new JsonMappingException(p, "Expected type 'entity_tree'");
                                }
                            }
                        }
                    }
                }
                case "root" -> root = p.readValueAs(EntityTreeHierarchy.Node.class);
                default -> p.skipChildren();
            }
        }

        throw new JsonMappingException(p, "EntityTreeHierarchy deserialization requires access to catalog context for proper reconstruction");
    }
}