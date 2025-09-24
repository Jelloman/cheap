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
        AspectDef aspectDef = (AspectDef) context.getAttribute("CheapAspectDef");
        if (aspectDef != null) {
            builder.aspectDef(aspectDef);
        }

        gen.writeStartObject();
        
        gen.writeStringField("name", propertyDef.name());
        gen.writeStringField("type", propertyDef.type().name());

        // Only write no-default values for all of the following fields

        if (propertyDef.hasDefaultValue()) {
            gen.writeFieldName("defaultValue");
            writeValue(propertyDef.defaultValue(), gen);
        }

        if (!propertyDef.isReadable()) {
            gen.writeBooleanField("isReadable", false);
        }
        gen.writeBooleanField("isWritable", propertyDef.isWritable());
        if (!propertyDef.isNullable()) {
            gen.writeBooleanField("isNullable", false);
        }
        gen.writeBooleanField("isRemovable", propertyDef.isRemovable());
        if (!propertyDef.isReadable()) {
            gen.writeBooleanField("isReadable", false);
        }
        gen.writeBooleanField("isMultivalued", propertyDef.isMultivalued());
        if (!propertyDef.isReadable()) {
            gen.writeBooleanField("isReadable", false);
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
        } else {
            gen.writeString(value.toString());
        }
    }
}