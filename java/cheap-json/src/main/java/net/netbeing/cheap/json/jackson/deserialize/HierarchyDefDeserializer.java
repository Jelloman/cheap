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

class HierarchyDefDeserializer extends JsonDeserializer<HierarchyDef>
{
    private final CheapFactory factory;

    public HierarchyDefDeserializer()
    {
        this(new CheapFactory());
    }

    public HierarchyDefDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public HierarchyDef deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        String name = null;
        HierarchyType type = null;
        boolean isModifiable = true;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "name" -> name = p.getValueAsString();
                case "type" -> {
                    String typeCode = p.getValueAsString().toUpperCase();
                    type = fromTypeCode(typeCode);
                }
                case "isModifiable" -> isModifiable = p.getBooleanValue();
                default -> p.skipChildren(); // Skip unknown fields
            }
        }

        if (name == null || type == null) {
            throw new JsonMappingException(p, "Missing required fields: name and type");
        }

        return factory.createHierarchyDef(name, type, isModifiable);
    }

    private HierarchyType fromTypeCode(String typeCode) throws JsonMappingException
    {
        return switch (typeCode) {
            case "EL" -> HierarchyType.ENTITY_LIST;
            case "ES" -> HierarchyType.ENTITY_SET;
            case "ED" -> HierarchyType.ENTITY_DIR;
            case "ET" -> HierarchyType.ENTITY_TREE;
            case "AM" -> HierarchyType.ASPECT_MAP;
            default -> throw new JsonMappingException(null, "Unknown hierarchy type code: " + typeCode);
        };
    }
}