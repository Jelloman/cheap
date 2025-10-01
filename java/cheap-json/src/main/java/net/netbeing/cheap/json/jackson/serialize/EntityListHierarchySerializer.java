package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

class EntityListHierarchySerializer extends JsonSerializer<EntityListHierarchy>
{
    @Override
    public void serialize(EntityListHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();

        gen.writeStringField("type", hierarchy.type().typeCode());
        gen.writeStringField("name", hierarchy.name());

        gen.writeFieldName("entities");
        gen.writeStartArray();
        for (Entity entity : hierarchy) {
            gen.writeString(entity.globalId().toString());
        }
        gen.writeEndArray();

        gen.writeEndObject();
    }
}