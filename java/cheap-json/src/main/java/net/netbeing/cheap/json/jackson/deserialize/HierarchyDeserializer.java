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

class HierarchyDeserializer extends JsonDeserializer<Hierarchy>
{
    private final CheapFactory factory;

    public HierarchyDeserializer()
    {
        this(new CheapFactory());
    }

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

        // Need to peek ahead to determine type
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            if ("def".equals(fieldName)) {
                if (p.currentToken() == JsonToken.START_OBJECT) {
                    while (p.nextToken() != JsonToken.END_OBJECT) {
                        String defField = p.currentName();
                        p.nextToken();
                        if ("type".equals(defField)) {
                            String typeValue = p.getValueAsString();
                            type = fromTypeValue(typeValue);
                            break;
                        }
                    }
                }
                break;
            }
        }

        if (type == null) {
            throw new JsonMappingException(p, "Unable to determine hierarchy type");
        }

        // Reset parser to beginning and deserialize as specific type
        // Note: This is a simplified approach that requires the type to be determinable from the def field

        return switch (type) {
            case ASPECT_MAP -> p.readValueAs(AspectMapHierarchy.class);
            case ENTITY_DIR -> p.readValueAs(EntityDirectoryHierarchy.class);
            case ENTITY_LIST -> p.readValueAs(EntityListHierarchy.class);
            case ENTITY_SET -> p.readValueAs(EntitySetHierarchy.class);
            case ENTITY_TREE -> p.readValueAs(EntityTreeHierarchy.class);
            default -> throw new JsonMappingException(p, "Unknown hierarchy type: " + type);
        };
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

    private HierarchyType fromTypeValue(String typeValue) throws JsonMappingException
    {
        return switch (typeValue) {
            case "entity_list" -> HierarchyType.ENTITY_LIST;
            case "entity_set" -> HierarchyType.ENTITY_SET;
            case "entity_dir" -> HierarchyType.ENTITY_DIR;
            case "entity_tree" -> HierarchyType.ENTITY_TREE;
            case "aspect_map" -> HierarchyType.ASPECT_MAP;
            default -> throw new JsonMappingException(null, "Unknown hierarchy type value: " + typeValue);
        };
    }
}