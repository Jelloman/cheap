package net.netbeing.cheap.json.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

class AspectDefDirHierarchyDeserializer extends JsonDeserializer<AspectDefDirHierarchy>
{
    private final CheapFactory factory;

    public AspectDefDirHierarchyDeserializer()
    {
        this(new CheapFactory());
    }

    public AspectDefDirHierarchyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public AspectDefDirHierarchy deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        HierarchyDef def = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "def" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            String defField = p.currentName();
                            p.nextToken();
                            if ("type".equals(defField)) {
                                String typeValue = p.getValueAsString();
                                if (!"aspect_def_dir".equals(typeValue)) {
                                    throw new JsonMappingException(p, "Expected type 'aspect_def_dir'");
                                }
                            }
                        }
                    }
                }
                case "aspectDefs" -> p.skipChildren();
                default -> p.skipChildren();
            }
        }

        throw new JsonMappingException(p, "AspectDefDirHierarchy deserialization requires access to catalog context for proper reconstruction");
    }
}