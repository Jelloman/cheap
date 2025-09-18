package net.netbeing.cheap.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

class EntityTreeHierarchySerializer extends JsonSerializer<EntityTreeHierarchy>
{
    @Override
    public void serialize(EntityTreeHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeFieldName("def");
        gen.writeStartObject();
        gen.writeStringField("type", "entity_tree");
        gen.writeEndObject();
        
        gen.writeFieldName("root");
        gen.writeObject(hierarchy.root());
        
        gen.writeEndObject();
    }
}