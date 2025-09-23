package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.util.Collection;

class PropertySerializer extends JsonSerializer<Property>
{
    @Override
    public void serialize(Property property, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeFieldName("def");
        gen.writeObject(property.def());
        
        gen.writeFieldName("value");
        writeValue(property.unsafeRead(), gen);
        
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