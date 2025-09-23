package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
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
        gen.writeBooleanField("strict", catalog.isStrict());

        if (catalog.upstream() != null) {
            gen.writeStringField("upstream", catalog.upstream().toString());
        }

        gen.writeFieldName("def");
        gen.writeObject(catalog.def());
        
        if (!catalog.isStrict()) {
            gen.writeFieldName("aspectDefs");
            gen.writeStartObject();
            for (AspectDef aspectDef : catalog.aspectDefs()) {
                // Elide those AspectDefs that are in the CatalogDef
                if (catalog.def().aspectDef(aspectDef.name()) == null) {
                    gen.writeFieldName(aspectDef.name());
                    gen.writeObject(aspectDef);
                }
            }
            gen.writeEndObject();
        }

        gen.writeFieldName("hierarchies");
        gen.writeStartObject();
        for (Hierarchy h : catalog.hierarchies()) {
            gen.writeFieldName(h.def().name());
            gen.writeObject(h);
        }
        gen.writeEndObject();
        
        gen.writeEndObject();
    }
}