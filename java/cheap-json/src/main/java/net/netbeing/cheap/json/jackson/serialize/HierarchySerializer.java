package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.util.Map;

class HierarchySerializer extends JsonSerializer<Hierarchy>
{
    @Override
    public void serialize(Hierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        HierarchyType type = hierarchy.type();

        gen.writeStartObject();

        gen.writeStringField("type", type.typeCode());
        gen.writeStringField("name", hierarchy.name());

        gen.writeFieldName("contents");

        switch (hierarchy) {
            case AspectMapHierarchy aMap -> serializeContent(aMap, gen, serializers);
            case EntityDirectoryHierarchy eDir -> serializeContent(eDir, gen, serializers);
            case EntityListHierarchy eList -> serializeContent(eList, gen, serializers);
            case EntitySetHierarchy eSet -> serializeContent(eSet, gen, serializers);
            case EntityTreeHierarchy eTree -> serializeContent(eTree, gen, serializers);
            default -> throw new IllegalArgumentException("Unknown hierarchy class: " + hierarchy.getClass());
        }

        gen.writeEndObject();
    }

    protected void serializeContent(AspectMapHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        for (Map.Entry<Entity, Aspect> entry : hierarchy.entrySet()) {
            gen.writeFieldName(entry.getKey().globalId().toString());
            gen.writeObject(entry.getValue());
        }
        gen.writeEndObject();
    }

    protected void serializeContent(EntityDirectoryHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        for (Map.Entry<String, Entity> entry : hierarchy.entrySet()) {
            gen.writeStringField(entry.getKey(), entry.getValue().globalId().toString());
        }
        gen.writeEndObject();
    }

    protected void serializeContent(EntityListHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartArray();
        for (Entity entity : hierarchy) {
            gen.writeString(entity.globalId().toString());
        }
        gen.writeEndArray();
    }

    protected void serializeContent(EntitySetHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartArray();
        for (Entity entity : hierarchy) {
            gen.writeString(entity.globalId().toString());
        }
        gen.writeEndArray();
    }

    protected void serializeContent(EntityTreeHierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeObject(hierarchy.root());
    }
}