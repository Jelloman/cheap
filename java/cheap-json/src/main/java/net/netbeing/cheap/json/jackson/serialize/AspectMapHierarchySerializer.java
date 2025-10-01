package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.util.Map;

class AspectMapHierarchySerializer extends JsonSerializer<AspectMapHierarchy>
{
    @Override
    public void serialize(AspectMapHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();

        gen.writeStringField("type", hierarchy.type().typeCode());
        gen.writeStringField("name", hierarchy.name());

        gen.writeFieldName("aspects");
        gen.writeStartObject();
        for (Map.Entry<Entity, Aspect> entry : hierarchy.entrySet()) {
            gen.writeFieldName(entry.getKey().globalId().toString());
            gen.writeObject(entry.getValue());
        }
        gen.writeEndObject();

        gen.writeEndObject();
    }
}