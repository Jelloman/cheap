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
import java.util.*;

/**
 * Jackson deserializer for {@link Aspect} objects in the Cheap data model.
 * <p>
 * This deserializer reconstructs an Aspect from JSON format by reading property values
 * and using an {@link AspectBuilder} from the {@link CheapFactory} to construct the
 * final Aspect instance. The deserializer requires access to both an AspectDef and Entity
 * reference, which can be provided through the JSON or via context attributes.
 * </p>
 * <p>
 * The deserializer supports two modes of operation:
 * </p>
 * <ul>
 *   <li>Standalone: JSON includes "aspectDefName" and "entityId" fields</li>
 *   <li>Contextual: AspectDef and Entity are provided via DeserializationContext attributes
 *       (used when deserializing as part of a hierarchy)</li>
 * </ul>
 * <p>
 * Property values are deserialized according to their PropertyType, with automatic type
 * conversion for numeric and boolean values that may be represented as strings in the JSON.
 * Multivalued properties are handled as JSON arrays.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonDeserializer}
 * and {@link HierarchyDeserializer}.
 * </p>
 *
 * @see Aspect
 * @see AspectBuilder
 * @see CheapFactory
 * @see HierarchyDeserializer
 */
class AspectDeserializer extends JsonDeserializer<Aspect>
{
    private final CheapFactory factory;

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

        AspectBuilder builder = factory.createAspectBuilder();
        AspectDef aspectDef = (AspectDef) context.getAttribute("CheapAspectDef");
        if (aspectDef != null) {
            builder.aspectDef(aspectDef);
        }
        Entity entity = (Entity) context.getAttribute("CheapEntity");
        if (entity != null) {
            builder.entity(entity);
        }

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "aspectDefName" -> {
                    aspectDef = factory.getAspectDef(p.getValueAsString());
                    if (aspectDef == null) {
                        throw new JsonMappingException(p, "AspectDef named '"+p.getValueAsString()+"' not found.");
                    }
                    builder.aspectDef(aspectDef);
                }
                case "entityId" -> {
                    UUID entityId = UUID.fromString(p.getValueAsString());
                    if (entity != null) {
                        if (!entity.globalId().equals(entityId)) {
                            throw new JsonMappingException(p, "Entity ID conflict in Aspect deserialization.");
                        }
                    } else {
                        entity = factory.getOrRegisterNewEntity(entityId);
                        builder.entity(entity);
                    }
                }
                default -> {
                    if (aspectDef == null) {
                        throw new JsonMappingException(p, "Attempted to read property named '"+fieldName+"' with any aspect or property definition.");
                    }
                    PropertyDef propDef = aspectDef.propertyDef(fieldName);
                    if (propDef == null) {
                        throw new JsonMappingException(p, "Property named '"+fieldName+"' was not found in aspect definition '"+aspectDef.name()+"'.");
                    }
                    Object value = readValue(p, propDef.type());
                    builder.property(fieldName, value);
                }
            }
        }

        if (entity == null) {
            throw new JsonMappingException(p, "Missing entity in Aspect deserialization.");
        }
        if (aspectDef == null) {
            throw new JsonMappingException(p, "Missing aspectDef in Aspect deserialization.");
        }

        return builder.build();
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
            case Integer -> {
                if (p.currentToken() == JsonToken.VALUE_STRING) {
                    // Handle string values that represent integers (common in JSON)
                    String value = p.getValueAsString();
                    yield Long.parseLong(value);
                } else {
                    yield p.getLongValue();
                }
            }
            case Float -> {
                if (p.currentToken() == JsonToken.VALUE_STRING) {
                    // Handle string values that represent floats
                    String value = p.getValueAsString();
                    yield Double.parseDouble(value);
                } else {
                    yield p.getDoubleValue();
                }
            }
            case Boolean -> {
                if (p.currentToken() == JsonToken.VALUE_STRING) {
                    // Handle string values that represent booleans
                    String value = p.getValueAsString();
                    yield Boolean.parseBoolean(value);
                } else if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                    yield p.getLongValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
                } else {
                    yield p.getBooleanValue();
                }
            }
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