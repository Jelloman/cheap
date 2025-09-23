package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.*;

import java.io.IOException;

class HierarchySerializer extends JsonSerializer<Hierarchy>
{
    @Override
    public void serialize(Hierarchy hierarchy, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        HierarchyType type = hierarchy.def().type();
        
        switch (type) {
            case ASPECT_MAP -> gen.writeObject((AspectMapHierarchy) hierarchy);
            case ENTITY_DIR -> gen.writeObject((EntityDirectoryHierarchy) hierarchy);
            case ENTITY_LIST -> gen.writeObject((EntityListHierarchy) hierarchy);
            case ENTITY_SET -> gen.writeObject((EntitySetHierarchy) hierarchy);
            case ENTITY_TREE -> gen.writeObject((EntityTreeHierarchy) hierarchy);
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + type);
        }
    }
}