package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

class AspectDefSerializer extends JsonSerializer<AspectDef>
{
    @Override
    public void serialize(AspectDef aspectDef, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeStringField("name", aspectDef.name());
        
        gen.writeFieldName("propertyDefs");
        gen.writeStartArray();
        for (PropertyDef propertyDef : aspectDef.propertyDefs()) {
            gen.writeObject(propertyDef);
        }
        gen.writeEndArray();
        
        gen.writeBooleanField("isReadable", aspectDef.isReadable());
        gen.writeBooleanField("isWritable", aspectDef.isWritable());
        gen.writeBooleanField("canAddProperties", aspectDef.canAddProperties());
        gen.writeBooleanField("canRemoveProperties", aspectDef.canRemoveProperties());
        
        gen.writeEndObject();
    }
}