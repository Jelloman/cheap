package net.netbeing.cheap.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.util.Map;

class EntityDirectoryHierarchySerializer extends JsonSerializer<EntityDirectoryHierarchy>
{
    @Override
    public void serialize(EntityDirectoryHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeFieldName("def");
        gen.writeStartObject();
        gen.writeStringField("type", "entity_dir");
        gen.writeEndObject();
        
        gen.writeFieldName("entities");
        gen.writeStartObject();
        for (Map.Entry<String, Entity> entry : hierarchy.entrySet()) {
            gen.writeStringField(entry.getKey(), entry.getValue().globalId().toString());
        }
        gen.writeEndObject();
        
        gen.writeEndObject();
    }
}