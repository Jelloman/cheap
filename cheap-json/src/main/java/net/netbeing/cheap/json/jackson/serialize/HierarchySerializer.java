/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityDirectoryHierarchy;
import net.netbeing.cheap.model.EntityListHierarchy;
import net.netbeing.cheap.model.EntitySetHierarchy;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyType;

import java.io.IOException;
import java.util.Map;

/**
 * Jackson serializer for {@link Hierarchy} objects in the Cheap data model.
 * <p>
 * This serializer converts any Hierarchy to JSON format by dispatching to
 * specialized serialization methods based on the hierarchy type (ENTITY_LIST,
 * ENTITY_SET, ENTITY_DIR, ENTITY_TREE, or ASPECT_MAP).
 * </p>
 * <p>
 * Each hierarchy type has a corresponding serialization method that produces
 * JSON appropriate for its structure:
 * </p>
 * <ul>
 *   <li>{@link AspectMapHierarchy}: Object mapping entity UUIDs to Aspect objects</li>
 *   <li>{@link EntityDirectoryHierarchy}: Object mapping string keys to entity UUIDs</li>
 *   <li>{@link EntityListHierarchy}: Array of entity UUIDs (may contain duplicates)</li>
 *   <li>{@link EntitySetHierarchy}: Array of unique entity UUIDs</li>
 *   <li>{@link EntityTreeHierarchy}: Recursive tree structure with entity values</li>
 * </ul>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonSerializer}
 * when serializing Catalog objects that contain Hierarchy collections.
 * </p>
 *
 * @see Hierarchy
 * @see HierarchyType
 * @see CatalogSerializer
 */
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