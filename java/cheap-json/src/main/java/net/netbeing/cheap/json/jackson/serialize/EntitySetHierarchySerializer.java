package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

class EntitySetHierarchySerializer extends JsonSerializer<EntitySetHierarchy>
{
    @Override
    public void serialize(EntitySetHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();

        // Only write the def as part of the hierarchy if it's not in the CatalogDef
        if (hierarchy.catalog().def().hierarchyDef(hierarchy.def().name()) == null) {
            gen.writeFieldName("def");
            gen.writeObject(hierarchy.def());
        }

        gen.writeFieldName("entities");
        gen.writeStartArray();
        for (Entity entity : hierarchy) {
            gen.writeString(entity.globalId().toString());
        }
        gen.writeEndArray();

        gen.writeEndObject();
    }
}