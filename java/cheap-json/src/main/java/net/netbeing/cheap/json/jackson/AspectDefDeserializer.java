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

class AspectDefDeserializer extends JsonDeserializer<AspectDef>
{
    private final CheapFactory factory;

    public AspectDefDeserializer()
    {
        this(new CheapFactory());
    }

    public AspectDefDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public AspectDef deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        String name = null;
        List<PropertyDef> propertyDefs = new ArrayList<>();
        boolean isReadable = true;
        boolean isWritable = true;
        boolean canAddProperties = false;
        boolean canRemoveProperties = false;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "name" -> name = p.getValueAsString();
                case "propertyDefs" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            propertyDefs.add(p.readValueAs(PropertyDef.class));
                        }
                    }
                }
                case "isReadable" -> isReadable = p.getBooleanValue();
                case "isWritable" -> isWritable = p.getBooleanValue();
                case "canAddProperties" -> canAddProperties = p.getBooleanValue();
                case "canRemoveProperties" -> canRemoveProperties = p.getBooleanValue();
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
        if (isWritable && canAddProperties && canRemoveProperties) {
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
}