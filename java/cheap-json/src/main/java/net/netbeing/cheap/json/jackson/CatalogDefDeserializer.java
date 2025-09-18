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
import java.util.ArrayList;
import java.util.List;

class CatalogDefDeserializer extends JsonDeserializer<CatalogDef>
{
    private final CheapFactory factory;

    public CatalogDefDeserializer()
    {
        this(new CheapFactory());
    }

    public CatalogDefDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public CatalogDef deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        List<AspectDef> aspectDefs = new ArrayList<>();
        List<HierarchyDef> hierarchyDefs = new ArrayList<>();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "aspectDefs" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            p.nextToken(); // Move to value
                            aspectDefs.add(p.readValueAs(AspectDef.class));
                        }
                    }
                }
                case "hierarchyDefs" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            hierarchyDefs.add(p.readValueAs(HierarchyDef.class));
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        return factory.createCatalogDef(hierarchyDefs, aspectDefs);
    }
}