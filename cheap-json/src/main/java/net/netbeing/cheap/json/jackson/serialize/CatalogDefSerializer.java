package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

class CatalogDefSerializer extends JsonSerializer<CatalogDef>
{
    @Override
    public void serialize(CatalogDef catalogDef, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeFieldName("aspectDefs");
        gen.writeStartObject();
        for (AspectDef aspectDef : catalogDef.aspectDefs()) {
            gen.writeFieldName(aspectDef.name());
            gen.writeObject(aspectDef);
        }
        gen.writeEndObject();
        
        gen.writeFieldName("hierarchyDefs");
        gen.writeStartArray();
        for (HierarchyDef hierarchyDef : catalogDef.hierarchyDefs()) {
            gen.writeObject(hierarchyDef);
        }
        gen.writeEndArray();
        
        gen.writeEndObject();
    }
}