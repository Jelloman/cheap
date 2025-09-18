package net.netbeing.cheap.json.jackson;

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
        
        gen.writeFieldName("def");
        gen.writeObject(catalog.def());
        
        if (catalog.upstream() != null) {
            gen.writeStringField("upstream", catalog.upstream().globalId().toString());
        }
        
        gen.writeFieldName("hierarchies");
        gen.writeStartObject();
        for (Map.Entry<String, Hierarchy> entry : catalog.hierarchies().entrySet()) {
            gen.writeFieldName(entry.getKey());
            gen.writeObject(entry.getValue());
        }
        gen.writeEndObject();
        
        gen.writeFieldName("aspectDefs");
        gen.writeStartObject();
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            gen.writeFieldName(aspectDef.name());
            gen.writeObject(aspectDef);
        }
        gen.writeEndObject();
        
        gen.writeFieldName("aspects");
        gen.writeStartObject();
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            AspectMapHierarchy aspectMap = catalog.aspects(aspectDef);
            if (aspectMap != null && !aspectMap.isEmpty()) {
                gen.writeFieldName(aspectDef.name());
                gen.writeStartObject();
                for (Map.Entry<Entity, Aspect> aspectEntry : aspectMap.entrySet()) {
                    gen.writeFieldName(aspectEntry.getKey().globalId().toString());
                    gen.writeObject(aspectEntry.getValue());
                }
                gen.writeEndObject();
            }
        }
        gen.writeEndObject();
        
        gen.writeEndObject();
    }
}