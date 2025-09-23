package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.util.Map;

class TreeNodeSerializer extends JsonSerializer<EntityTreeHierarchy.Node>
{
    @Override
    public void serialize(EntityTreeHierarchy.Node node, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        if (node.value() != null) {
            gen.writeStringField("entityId", node.value().globalId().toString());
        }
        
        if (!node.isEmpty()) {
            gen.writeFieldName("children");
            gen.writeStartObject();
            for (Map.Entry<String, EntityTreeHierarchy.Node> entry : node.entrySet()) {
                gen.writeFieldName(entry.getKey());
                gen.writeObject(entry.getValue());
            }
            gen.writeEndObject();
        }
        
        gen.writeEndObject();
    }
}