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
import java.util.ArrayList;
import java.util.List;

class PropertyDeserializer extends JsonDeserializer<Property>
{
    private final CheapFactory factory;

    public PropertyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public Property deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        AspectDef aspectDef = (AspectDef) context.getAttribute("CheapAspectDef");
        if (aspectDef == null) {
            throw new JsonMappingException(p, "Property cannot be deserialized without an AspectDef.");
        }
        PropertyDef def = null;
        Object value = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> def = p.readValueAs(PropertyDef.class);
                case "value" -> {
                    if (def != null) {
                        value = readValue(p, def.type());
                    } else {
                        value = readValue(p, null);
                    }
                }
                default -> p.skipChildren();
            }
        }

        if (def == null) {
            throw new JsonMappingException(p, "Missing required field: def");
        }

        throw new JsonMappingException(p, "Property deserialization requires access to aspect context for proper reconstruction");
    }

    private Object readValue(JsonParser p, @NotNull PropertyType type) throws IOException
    {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        return switch (type) {
            case String, Text, BigInteger, DateTime, URI, UUID -> p.getValueAsString();
            case Integer -> p.getLongValue();
            case Float -> p.getDoubleValue();
            case Boolean -> p.getBooleanValue();
            default -> {
                if (p.currentToken() == JsonToken.START_ARRAY) {
                    List<Object> list = new ArrayList<>();
                    while (p.nextToken() != JsonToken.END_ARRAY) {
                        list.add(readValue(p, type));
                    }
                    yield list;
                } else {
                    yield p.getValueAsString();
                }
            }
        };
    }
}