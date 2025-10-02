package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;

class AspectSerializer extends JsonSerializer<Aspect>
{
    private final boolean includeEntityId;
    private final boolean includeAspectDefName;

    public AspectSerializer()
    {
        this(false, false);
    }

    public AspectSerializer(boolean includeEntityId, boolean includeAspectDefName)
    {
        this.includeEntityId = includeEntityId;
        this.includeAspectDefName = includeAspectDefName;
    }

    @Override
    public void serialize(Aspect aspect, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();

        if (includeAspectDefName) {
            gen.writeStringField("aspectDefName", aspect.def().name());
        }
        if (includeEntityId) {
            gen.writeStringField("entityId", aspect.entity().globalId().toString());
        }

        // Add all properties
        for (PropertyDef propertyDef : aspect.def().propertyDefs()) {
            Object value = aspect.readObj(propertyDef.name());
            if (value != null) {
                gen.writeFieldName(propertyDef.name());
                writeValue(value, gen);
            }
        }
        
        gen.writeEndObject();
    }
    
    private void writeValue(Object value, JsonGenerator gen) throws IOException
    {
        if (value == null) {
            gen.writeNull();
        } else if (value instanceof String) {
            gen.writeString((String) value);
        } else if (value instanceof Number) {
            gen.writeNumber(value.toString());
        } else if (value instanceof Boolean) {
            gen.writeBoolean((Boolean) value);
        } else if (value instanceof Collection<?> collection) {
            gen.writeStartArray();
            for (Object item : collection) {
                writeValue(item, gen);
            }
            gen.writeEndArray();
        } else if (value.getClass().isArray()) {
            writeValue(value, gen);
        } else {
            gen.writeString(value.toString());
        }
    }
}