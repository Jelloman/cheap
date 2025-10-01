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
            case AspectMapHierarchy aMap -> serializeContent(aMap, gen);
            case EntityDirectoryHierarchy eDir -> serializeContent(eDir, gen);
            case EntityListHierarchy eList -> serializeContent(eList, gen);
            case EntitySetHierarchy eSet -> serializeContent(eSet, gen);
            case EntityTreeHierarchy eTree -> serializeContent(eTree, gen);
            default -> throw new IllegalArgumentException("Unknown hierarchy class: " + hierarchy.getClass());
        }

        gen.writeEndObject();
    }

    protected void serializeContent(AspectMapHierarchy hierarchy, JsonGenerator gen) throws IOException
    {
        gen.writeStartObject();
        for (Map.Entry<Entity, Aspect> entry : hierarchy.entrySet()) {
            gen.writeFieldName(entry.getKey().globalId().toString());
            gen.writeObject(entry.getValue());
        }
        gen.writeEndObject();
    }

    protected void serializeContent(EntityDirectoryHierarchy hierarchy, JsonGenerator gen) throws IOException
    {
        gen.writeStartObject();
        for (Map.Entry<String, Entity> entry : hierarchy.entrySet()) {
            gen.writeStringField(entry.getKey(), entry.getValue().globalId().toString());
        }
        gen.writeEndObject();
    }

    protected void serializeContent(EntityListHierarchy hierarchy, JsonGenerator gen) throws IOException
    {
        gen.writeStartArray();
        for (Entity entity : hierarchy) {
            gen.writeString(entity.globalId().toString());
        }
        gen.writeEndArray();
    }

    protected void serializeContent(EntitySetHierarchy hierarchy, JsonGenerator gen) throws IOException
    {
        gen.writeStartArray();
        for (Entity entity : hierarchy) {
            gen.writeString(entity.globalId().toString());
        }
        gen.writeEndArray();
    }

    protected void serializeContent(EntityTreeHierarchy hierarchy, JsonGenerator gen) throws IOException
    {
        serializeNode(hierarchy.root(), gen);
    }

    protected void serializeNode(EntityTreeHierarchy.Node node, JsonGenerator gen) throws IOException
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
                serializeNode(entry.getValue(), gen);
            }
            gen.writeEndObject();
        }

        gen.writeEndObject();
    }
}