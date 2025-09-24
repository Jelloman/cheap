package net.netbeing.cheap.json.jackson.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

class CatalogDeserializer extends JsonDeserializer<Catalog>
{
    private final CheapFactory factory;

    public CatalogDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public Catalog deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        UUID globalId = null;
        URI uri = null;
        CatalogSpecies species = null;
        boolean strict = false;
        CatalogDef def = null;
        UUID upstreamId = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "globalId" -> globalId = UUID.fromString(p.getValueAsString());
                case "uri" -> {
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        uri = URI.create(p.getValueAsString());
                    }
                }
                case "species" -> species = CatalogSpecies.valueOf(p.getValueAsString().toUpperCase());
                case "strict" -> strict = p.getBooleanValue();
                case "def" -> def = p.readValueAs(CatalogDef.class);
                case "upstream" -> {
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        upstreamId = UUID.fromString(p.getValueAsString());
                    }
                }
                case "hierarchies", "aspectDefs", "aspects" -> p.skipChildren();
                default -> p.skipChildren();
            }
        }

        if (globalId == null || species == null || def == null) {
            throw new JsonMappingException(p, "Missing required fields: globalId, species, and def");
        }

        return factory.createCatalog(globalId, species, def, null, strict);
    }
}