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

class PropertyDefDeserializer extends JsonDeserializer<PropertyDef>
{
    private final CheapFactory factory;

    public PropertyDefDeserializer()
    {
        this(new CheapFactory());
    }

    public PropertyDefDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public PropertyDef deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        String name = null;
        PropertyType type = null;
        Object defaultValue = null;
        boolean hasDefaultValue = false;
        boolean isReadable = true;
        boolean isWritable = true;
        boolean isNullable = false;
        boolean isRemovable = false;
        boolean isMultivalued = false;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "name" -> name = p.getValueAsString();
                case "type" -> type = PropertyType.valueOf(p.getValueAsString());
                case "hasDefaultValue" -> hasDefaultValue = p.getBooleanValue();
                case "defaultValue" -> defaultValue = readValue(p, type);
                case "isReadable" -> isReadable = p.getBooleanValue();
                case "isWritable" -> isWritable = p.getBooleanValue();
                case "isNullable" -> isNullable = p.getBooleanValue();
                case "isRemovable" -> isRemovable = p.getBooleanValue();
                case "isMultivalued" -> isMultivalued = p.getBooleanValue();
                default -> p.skipChildren(); // Skip unknown fields
            }
        }

        if (name == null || type == null) {
            throw new JsonMappingException(p, "Missing required fields: name and type");
        }

        return factory.createPropertyDef(name, type, defaultValue, hasDefaultValue,
                                       isReadable, isWritable, isNullable, isRemovable, isMultivalued);
    }

    private Object readValue(JsonParser p, PropertyType type) throws IOException
    {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        if (type == null) {
            return p.getValueAsString();
        }

        return switch (type) {
            case Integer -> p.getLongValue();
            case Float -> p.getDoubleValue();
            case Boolean -> p.getBooleanValue();
            default -> p.getValueAsString();
        };
    }
}