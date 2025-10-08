package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;

/**
 * Jackson serializer for {@link Aspect} objects in the Cheap data model.
 * <p>
 * This serializer converts an Aspect to JSON format, including all of its properties
 * as defined by its AspectDef. The serializer can optionally include the aspect
 * definition name and entity ID in the output based on configuration flags.
 * </p>
 * <p>
 * The serializer iterates through all property definitions in the aspect's AspectDef
 * and writes the corresponding property values to JSON. Only non-null property values
 * are included in the output to keep the JSON concise.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonSerializer}
 * and {@link HierarchySerializer} when serializing AspectMapHierarchy contents.
 * </p>
 *
 * @see Aspect
 * @see AspectDef
 * @see CheapJacksonSerializer
 */
class AspectSerializer extends JsonSerializer<Aspect>
{
    private final boolean includeEntityId;
    private final boolean includeAspectDefName;

    public AspectSerializer()
    {
        this(false, false);
    }

    public AspectSerializer(boolean includeEntityId, boolean includeAspectDefName)
    {
        this.includeEntityId = includeEntityId;
        this.includeAspectDefName = includeAspectDefName;
    }

    @Override
    public void serialize(Aspect aspect, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();

        if (includeAspectDefName) {
            gen.writeStringField("aspectDefName", aspect.def().name());
        }
        if (includeEntityId) {
            gen.writeStringField("entityId", aspect.entity().globalId().toString());
        }

        // Add all properties
        for (PropertyDef propertyDef : aspect.def().propertyDefs()) {
            Object value = aspect.readObj(propertyDef.name());
            if (value != null) {
                gen.writeFieldName(propertyDef.name());
                writeValue(value, gen);
            }
        }
        
        gen.writeEndObject();
    }
    
    private void writeValue(Object value, JsonGenerator gen) throws IOException
    {
        if (value == null) {
            gen.writeNull();
        } else if (value instanceof String) {
            gen.writeString((String) value);
        } else if (value instanceof Number) {
            gen.writeNumber(value.toString());
        } else if (value instanceof Boolean) {
            gen.writeBoolean((Boolean) value);
        } else if (value instanceof Collection<?> collection) {
            gen.writeStartArray();
            for (Object item : collection) {
                writeValue(item, gen);
            }
            gen.writeEndArray();
        } else if (value.getClass().isArray()) {
            writeValue(value, gen);
        } else {
            gen.writeString(value.toString());
        }
    }
}