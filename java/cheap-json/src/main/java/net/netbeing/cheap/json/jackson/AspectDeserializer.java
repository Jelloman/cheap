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
import java.util.*;

class AspectDeserializer extends JsonDeserializer<Aspect>
{
    private final CheapFactory factory;

    public AspectDeserializer()
    {
        this(new CheapFactory());
    }

    public AspectDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public Aspect deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        AspectDef aspectDef = (AspectDef) context.getAttribute("CheapAspectDef");
        Entity entity = (Entity) context.getAttribute("CheapEntity");
        Aspect aspect = null;
        Map<String,String> propValues = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "aspectDefName" -> {
                    aspectDef = factory.getAspectDef(p.getValueAsString());
                    if (aspectDef == null) {
                        throw new JsonMappingException(p, "AspectDef named '"+p.getValueAsString()+"' not found.");
                    }
                }
                case "entityId" -> {
                    UUID entityId = UUID.fromString(p.getValueAsString());
                    if (entity != null) {
                        if (!entity.globalId().equals(entityId)) {
                            throw new JsonMappingException(p, "Entity ID conflict in Aspect deserialization.");
                        }
                    } else {
                        entity = factory.getOrRegisterNewEntity(entityId);
                    }
                }
                default -> {
                    // FIXME: write and use AspectBuilder via the factory.
                    if (aspect != null) {
                        aspect.write(fieldName, p.getValueAsString());
                    }
                    propValues.put(fieldName, p.getValueAsString());
                }
            }
        }

        if (entity == null) {
            throw new JsonMappingException(p, "Missing entity in Aspect deserialization.");
        }
        if (aspectDef == null) {
            throw new JsonMappingException(p, "Missing aspectDef in Aspect deserialization.");
        }

        throw new JsonMappingException(p, "Aspect deserialization requires access to catalog context for proper reconstruction");
    }

    @SuppressWarnings("unchecked")
    private Object readValue(JsonParser p, PropertyType type) throws IOException
    {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        if (type == null) {
            return p.getValueAsString();
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