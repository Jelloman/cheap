package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

class HierarchyDefSerializer extends JsonSerializer<HierarchyDef>
{
    @Override
    public void serialize(HierarchyDef hierarchyDef, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeStringField("name", hierarchyDef.name());
        gen.writeStringField("type", hierarchyDef.type().typeCode().toLowerCase());
        gen.writeBooleanField("isModifiable", hierarchyDef.isModifiable());
        
        gen.writeEndObject();
    }
}