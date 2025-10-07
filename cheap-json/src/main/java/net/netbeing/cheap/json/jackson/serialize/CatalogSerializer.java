package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.util.Map;

class CatalogSerializer extends JsonSerializer<Catalog>
{
    @Override
    public void serialize(Catalog catalog, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeStringField("globalId", catalog.globalId().toString());
        
        if (catalog.uri() != null) {
            gen.writeStringField("uri", catalog.uri().toString());
        } else {
            gen.writeNullField("uri");
        }
        
        gen.writeStringField("species", catalog.species().name().toLowerCase());
        if (catalog.upstream() != null) {
            gen.writeStringField("upstream", catalog.upstream().toString());
        }

        gen.writeFieldName("aspectDefs");
        gen.writeStartObject();
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            gen.writeFieldName(aspectDef.name());
            gen.writeObject(aspectDef);
        }
        gen.writeEndObject();

        gen.writeFieldName("hierarchies");
        gen.writeStartObject();
        for (Hierarchy h : catalog.hierarchies()) {
            gen.writeFieldName(h.name());
            gen.writeObject(h);
        }
        gen.writeEndObject();
        
        gen.writeEndObject();
    }
}