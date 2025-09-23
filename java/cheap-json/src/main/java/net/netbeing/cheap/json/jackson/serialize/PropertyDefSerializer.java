package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

class PropertyDefSerializer extends JsonSerializer<PropertyDef>
{
    @Override
    public void serialize(PropertyDef propertyDef, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeStringField("name", propertyDef.name());
        gen.writeStringField("type", propertyDef.type().name());
        
        if (propertyDef.hasDefaultValue()) {
            gen.writeBooleanField("hasDefaultValue", true);
            gen.writeFieldName("defaultValue");
            writeValue(propertyDef.defaultValue(), gen);
        } else {
            gen.writeBooleanField("hasDefaultValue", false);
        }
        
        gen.writeBooleanField("isReadable", propertyDef.isReadable());
        gen.writeBooleanField("isWritable", propertyDef.isWritable());
        gen.writeBooleanField("isNullable", propertyDef.isNullable());
        gen.writeBooleanField("isRemovable", propertyDef.isRemovable());
        gen.writeBooleanField("isMultivalued", propertyDef.isMultivalued());
        
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
        } else {
            gen.writeString(value.toString());
        }
    }
}