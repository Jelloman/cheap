package net.netbeing.cheap.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.util.Collection;

class AspectSerializer extends JsonSerializer<Aspect>
{
    @Override
    public void serialize(Aspect aspect, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeStringField("aspectDefName", aspect.def().name());
        gen.writeStringField("entityId", aspect.entity().globalId().toString());
        gen.writeBooleanField("isTransferable", aspect.isTransferable());
        
        // Add all properties
        for (PropertyDef propertyDef : aspect.def().propertyDefs()) {
            Object value = aspect.unsafeReadObj(propertyDef.name());
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
        } else {
            gen.writeString(value.toString());
        }
    }
}