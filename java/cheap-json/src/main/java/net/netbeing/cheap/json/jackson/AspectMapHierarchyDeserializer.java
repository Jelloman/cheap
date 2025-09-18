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

class AspectMapHierarchyDeserializer extends JsonDeserializer<AspectMapHierarchy>
{
    private final CheapFactory factory;

    public AspectMapHierarchyDeserializer()
    {
        this(new CheapFactory());
    }

    public AspectMapHierarchyDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public AspectMapHierarchy deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        String aspectDefName = null;

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
                                if (!"aspect_map".equals(typeValue)) {
                                    throw new JsonMappingException(p, "Expected type 'aspect_map'");
                                }
                            }
                        }
                    }
                }
                case "aspectDefName" -> aspectDefName = p.getValueAsString();
                case "aspects" -> p.skipChildren();
                default -> p.skipChildren();
            }
        }

        throw new JsonMappingException(p, "AspectMapHierarchy deserialization requires access to catalog context for proper reconstruction");
    }
}