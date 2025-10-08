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

/**
 * Jackson deserializer for {@link HierarchyDef} objects in the Cheap data model.
 * <p>
 * This deserializer reconstructs a HierarchyDef from JSON format by reading its name
 * and type code. HierarchyDef is a simple metadata structure that specifies the name
 * and organizational type of a hierarchy.
 * </p>
 * <p>
 * The deserializer converts two-character type codes (EL, ES, ED, ET, AM) to the
 * corresponding {@link HierarchyType} enumeration values. Legacy fields like
 * "isModifiable" are gracefully ignored for backward compatibility.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CatalogDefDeserializer}
 * when deserializing catalog definitions from JSON.
 * </p>
 *
 * @see HierarchyDef
 * @see HierarchyType
 * @see CatalogDefDeserializer
 * @see CheapFactory
 */
class HierarchyDefDeserializer extends JsonDeserializer<HierarchyDef>
{
    private final CheapFactory factory;

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

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "name" -> name = p.getValueAsString();
                case "type" -> {
                    String typeCode = p.getValueAsString().toUpperCase();
                    type = fromTypeCode(typeCode);
                }
                case "isModifiable" -> p.skipChildren(); // isModifiable no longer exists
                default -> p.skipChildren(); // Skip unknown fields
            }
        }

        if (name == null || type == null) {
            throw new JsonMappingException(p, "Missing required fields: name and type");
        }

        return factory.createHierarchyDef(name, type);
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