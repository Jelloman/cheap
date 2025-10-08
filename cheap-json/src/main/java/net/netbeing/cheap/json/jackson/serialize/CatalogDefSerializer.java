package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

/**
 * Jackson serializer for {@link CatalogDef} objects in the Cheap data model.
 * <p>
 * This serializer converts a CatalogDef to JSON format, including its collections
 * of AspectDef and HierarchyDef objects. A CatalogDef is purely informational and
 * defines the structure of data types that a catalog typically contains.
 * </p>
 * <p>
 * The serializer outputs AspectDefs as an object (map) keyed by aspect name,
 * and HierarchyDefs as an array. This structure matches the lookup patterns
 * commonly used when working with catalog definitions.
 * </p>
 * <p>
 * This class is package-private and may be used internally by {@link CheapJacksonSerializer}
 * for standalone CatalogDef serialization or as part of metadata exports.
 * </p>
 *
 * @see CatalogDef
 * @see AspectDef
 * @see HierarchyDef
 * @see CheapJacksonSerializer
 */
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