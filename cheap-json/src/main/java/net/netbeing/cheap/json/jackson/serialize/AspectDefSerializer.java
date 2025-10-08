package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

/**
 * Jackson serializer for {@link AspectDef} objects in the Cheap data model.
 * <p>
 * This serializer converts an AspectDef to JSON format, including its name,
 * access control flags (isReadable, isWritable, canAddProperties, canRemoveProperties),
 * and the collection of PropertyDef specifications.
 * </p>
 * <p>
 * The serializer optimizes the JSON output by omitting boolean flags that have
 * their default values (true for isReadable/isWritable, false for canAddProperties/canRemoveProperties).
 * This reduces JSON verbosity while maintaining full fidelity.
 * </p>
 * <p>
 * Property definitions are serialized inline as an array of objects, with each
 * PropertyDef containing its name, type, and any non-default configuration flags.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonSerializer}
 * when serializing Catalog objects that contain AspectDef collections.
 * </p>
 *
 * @see AspectDef
 * @see PropertyDef
 * @see CatalogSerializer
 */
class AspectDefSerializer extends JsonSerializer<AspectDef>
{
    @Override
    public void serialize(AspectDef aspectDef, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        
        gen.writeStringField("name", aspectDef.name());

        // Only serialize these flags when they are non-default
        if (!aspectDef.isReadable()) {
            gen.writeBooleanField("isReadable", aspectDef.isReadable());
        }
        if (!aspectDef.isWritable()) {
            gen.writeBooleanField("isWritable", aspectDef.isWritable());
        }
        if (aspectDef.canAddProperties()) {
            gen.writeBooleanField("canAddProperties", aspectDef.canAddProperties());
        }
        if (aspectDef.canRemoveProperties()) {
            gen.writeBooleanField("canRemoveProperties", aspectDef.canRemoveProperties());
        }

        gen.writeFieldName("propertyDefs");
        gen.writeStartArray();
        for (PropertyDef propertyDef : aspectDef.propertyDefs()) {
            gen.writeStartObject();

            gen.writeStringField("name", propertyDef.name());
            gen.writeStringField("type", propertyDef.type().name());

            // Only write no-default values for all the following fields

            if (propertyDef.hasDefaultValue()) {
                gen.writeFieldName("defaultValue");
                writeValue(propertyDef.defaultValue(), gen);
            }

            if (propertyDef.isReadable() != aspectDef.isReadable()) {
                gen.writeBooleanField("isReadable", propertyDef.isReadable());
            }
            if (propertyDef.isWritable() != aspectDef.isWritable()) {
                gen.writeBooleanField("isWritable", propertyDef.isWritable());
            }
            if (!propertyDef.isNullable()) {
                gen.writeBooleanField("isNullable", false);
            }
            if (aspectDef.canRemoveProperties() && propertyDef.isRemovable()) {
                gen.writeBooleanField("isRemovable", true);
            }
            if (propertyDef.isMultivalued()) {
                gen.writeBooleanField("isMultivalued", true);
            }

            gen.writeEndObject();
        }
        gen.writeEndArray();
        
        gen.writeEndObject();
    }

    private void writeValue(Object value, JsonGenerator gen) throws IOException
    {
        switch (value) {
            case null -> gen.writeNull();
            case String s -> gen.writeString(s);
            case Number number -> gen.writeNumber(value.toString());
            case Boolean b -> gen.writeBoolean(b);
            default -> gen.writeString(value.toString());
        }
    }
}