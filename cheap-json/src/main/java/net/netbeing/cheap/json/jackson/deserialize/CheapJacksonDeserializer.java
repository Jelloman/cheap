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

package net.netbeing.cheap.json.jackson.deserialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;

/**
 * Jackson-based JSON deserializer for Cheap data model objects.
 * Provides deserialization capabilities using Jackson with custom deserializers
 * that can be configured with a CheapFactory for object creation.
 *
 * <p>This class can be used in two modes:</p>
 * <ul>
 *   <li>Default mode: Uses a static ObjectMapper with a default CheapFactory</li>
 *   <li>Factory mode: Creates a new ObjectMapper configured with the provided CheapFactory</li>
 * </ul>
 *
 * <p>Uses custom Jackson deserializers for each Cheap element type that leverage
 * the CheapFactory for consistent object creation.</p>
 */
public class CheapJacksonDeserializer
{
    private static final ObjectMapper DEFAULT_MAPPER;
    private static final CheapFactory DEFAULT_FACTORY = new CheapFactory();

    static {
        DEFAULT_MAPPER = createMapper(DEFAULT_FACTORY);
    }

    private final ObjectMapper mapper;
    private final CheapFactory factory;

    /**
     * Creates a new deserializer using the default CheapFactory and static ObjectMapper.
     * This is the most efficient option for typical use cases.
     */
    public CheapJacksonDeserializer()
    {
        this.mapper = DEFAULT_MAPPER;
        this.factory = DEFAULT_FACTORY;
    }

    /**
     * Creates a new deserializer with the specified CheapFactory.
     * This creates a new ObjectMapper configured with deserializers that use the provided factory.
     *
     * @param factory the CheapFactory to use for object creation during deserialization
     */
    public CheapJacksonDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
        this.mapper = createMapper(factory);
    }

    /**
     * Returns the CheapFactory used by this deserializer.
     *
     * @return the CheapFactory instance
     */
    public @NotNull CheapFactory getFactory()
    {
        return factory;
    }

    /**
     * Deserializes a JSON string to a Catalog object.
     *
     * @param json the JSON string to deserialize
     * @return the deserialized Catalog
     * @throws JsonProcessingException if the JSON is invalid or cannot be deserialized
     */
    public @NotNull Catalog fromJson(@NotNull String json) throws JsonProcessingException
    {
        return mapper.readValue(json, Catalog.class);
    }

    /**
     * Deserializes JSON from a reader to a Catalog object.
     *
     * @param reader the reader containing JSON data
     * @return the deserialized Catalog
     * @throws IOException if the JSON is invalid or cannot be deserialized
     */
    public @NotNull Catalog fromJson(@NotNull Reader reader) throws IOException
    {
        return mapper.readValue(reader, Catalog.class);
    }

    /**
     * Creates and configures an ObjectMapper with custom deserializers for Cheap types.
     * The deserializers are configured with the provided CheapFactory for object creation.
     */
    private static ObjectMapper createMapper(@NotNull CheapFactory factory)
    {
        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule("CheapDeserializerModule");

        // Register custom deserializers for each Cheap type
        module.addDeserializer(Catalog.class, new CatalogDeserializer(factory));
        module.addDeserializer(CatalogDef.class, new CatalogDefDeserializer(factory));
        module.addDeserializer(AspectDef.class, new AspectDefDeserializer(factory));
        module.addDeserializer(PropertyDef.class, new PropertyDefDeserializer(factory));
        module.addDeserializer(HierarchyDef.class, new HierarchyDefDeserializer(factory));
        module.addDeserializer(Hierarchy.class, new HierarchyDeserializer(factory));
        module.addDeserializer(Aspect.class, new AspectDeserializer(factory));
        module.addDeserializer(EntityTreeHierarchy.Node.class, new TreeNodeDeserializer(factory));

        mapper.registerModule(module);

        return mapper;
    }
}