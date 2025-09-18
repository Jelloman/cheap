package net.netbeing.cheap.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.util.Map;

class HierarchyDirSerializer extends JsonSerializer<HierarchyDir>
{
    @Override
    public void serialize(HierarchyDir hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeFieldName("def");
        gen.writeStartObject();
        gen.writeStringField("type", "hierarchy_dir");
        gen.writeEndObject();
        
        gen.writeFieldName("hierarchies");
        gen.writeStartObject();
        for (Map.Entry<String, Hierarchy> entry : hierarchy.entrySet()) {
            gen.writeStringField(entry.getKey(), entry.getValue().def().name());
        }
        gen.writeEndObject();
        
        gen.writeEndObject();
    }
}