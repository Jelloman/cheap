package net.netbeing.cheap.json.jackson.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class AspectDefDeserializer extends JsonDeserializer<AspectDef>
{
    private final CheapFactory factory;

    public AspectDefDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    private static final class Flags
    {
        boolean isReadable = true;
        boolean isWritable = true;
        boolean canAddProperties = false;
        boolean canRemoveProperties = false;
    }

    @Override
    public AspectDef deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token to begin AspectDef");
        }

        String name = null;
        List<PropertyDef> propertyDefs = new ArrayList<>();
        Flags flags = new Flags();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "name" -> name = p.getValueAsString();
                case "isReadable" -> flags.isReadable = p.getBooleanValue();
                case "isWritable" -> flags.isWritable = p.getBooleanValue();
                case "canAddProperties" -> flags.canAddProperties = p.getBooleanValue();
                case "canRemoveProperties" -> flags.canRemoveProperties = p.getBooleanValue();
                case "propertyDefs" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            PropertyDef propDef = deserializePropertyDef(p, flags);
                            propertyDefs.add(propDef);
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        if (name == null) {
            throw new JsonMappingException(p, "Missing required field in AspectDef: name");
        }

        Map<String, PropertyDef> propertyDefMap = new LinkedHashMap<>();
        for (PropertyDef propertyDef : propertyDefs) {
            propertyDefMap.put(propertyDef.name(), propertyDef);
        }

        // TODO: add factory method with all fields
        AspectDef def = null;
        if (flags.isWritable && flags.canAddProperties && flags.canRemoveProperties) {
            def = factory.createMutableAspectDef(name, propertyDefMap);
        } else {
            def = factory.createImmutableAspectDef(name, propertyDefMap);
        }
        AspectDef existingDef = factory.getAspectDef(name);
        if (existingDef != null) {
            if (!existingDef.fullyEquals(def)) {
                throw new JsonMappingException(p, "Attempted to deserialize AspectDef " + name + " that conflicts with the AspectDef already registered with that name.");
            }
        } else {
            factory.registerAspectDef(def);
        }

        return def;
    }

    private PropertyDef deserializePropertyDef(JsonParser p, Flags flags) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token to start PropertyDef");
        }

        String name = null;
        PropertyType type = null;
        Object defaultValue = null;
        boolean hasDefaultValue = false;
        boolean isReadable = flags.isReadable;
        boolean isWritable = flags.isWritable;
        boolean isNullable = true;
        boolean isRemovable = flags.canRemoveProperties;
        boolean isMultivalued = false;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "name" -> name = p.getValueAsString();
                case "type" -> type = PropertyType.valueOf(p.getValueAsString());
                case "defaultValue" -> {
                    defaultValue = readValue(p, type);
                    hasDefaultValue = true;
                }
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

        return factory.createPropertyDef(name, type, defaultValue, hasDefaultValue, isReadable,
            isWritable, isNullable, isRemovable, isMultivalued);
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