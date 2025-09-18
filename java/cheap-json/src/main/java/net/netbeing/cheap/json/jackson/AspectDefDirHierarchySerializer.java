package net.netbeing.cheap.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

class AspectDefDirHierarchySerializer extends JsonSerializer<AspectDefDirHierarchy>
{
    @Override
    public void serialize(AspectDefDirHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeFieldName("def");
        gen.writeStartObject();
        gen.writeStringField("type", "aspect_def_dir");
        gen.writeEndObject();
        
        gen.writeFieldName("aspectDefs");
        gen.writeStartObject();
        for (AspectDef aspectDef : hierarchy.aspectDefs()) {
            gen.writeFieldName(aspectDef.name());
            gen.writeObject(aspectDef);
        }
        gen.writeEndObject();
        
        gen.writeEndObject();
    }
}