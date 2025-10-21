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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyDef;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Jackson-based JSON serializer for Cheap data model objects.
 * Provides the same functionality as CheapJsonRawSerializer but uses Jackson
 * for more flexible and extensible JSON generation.
 * 
 * <p>This class provides methods for serializing Cheap model objects to JSON format,
 * as defined by the JSON schemas in this module.</p>
 * 
 * <p>Uses custom Jackson serializers for each Cheap element type since
 * no Jackson annotations are used on the model classes.</p>
 */
public class CheapJacksonSerializer
{
    private static final ObjectMapper DEFAULT_MAPPER;
    private static final ObjectMapper PRETTY_MAPPER;
    
    static {
        DEFAULT_MAPPER = createMapper(false);
        PRETTY_MAPPER = createMapper(true);
    }
    
    private CheapJacksonSerializer()
    {
        // Utility class - prevent instantiation
    }
    
    /**
     * Main entry point - converts a Catalog to JSON string.
     * 
     * @param catalog the catalog to convert
     * @return JSON string representation of the catalog
     */
    public static String toJson(Catalog catalog)
    {
        return toJson(catalog, false);
    }
    
    /**
     * Main entry point - converts a Catalog to JSON string with optional pretty printing.
     * 
     * @param catalog the catalog to convert
     * @param prettyPrint whether to format with newlines and indentation
     * @return JSON string representation of the catalog
     */
    public static String toJson(Catalog catalog, boolean prettyPrint)
    {
        try {
            ObjectMapper mapper = prettyPrint ? PRETTY_MAPPER : DEFAULT_MAPPER;
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, catalog);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize catalog to JSON", e);
        }
    }
    
    /**
     * Creates and configures an ObjectMapper with custom serializers for Cheap types.
     */
    private static ObjectMapper createMapper(boolean prettyPrint)
    {
        ObjectMapper mapper = new ObjectMapper();
        
        if (prettyPrint) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        
        SimpleModule module = new SimpleModule("CheapModule");
        
        // Register custom serializers for each Cheap type
        module.addSerializer(Catalog.class, new CatalogSerializer());
        module.addSerializer(CatalogDef.class, new CatalogDefSerializer());
        module.addSerializer(AspectDef.class, new AspectDefSerializer());
        module.addSerializer(HierarchyDef.class, new HierarchyDefSerializer());
        module.addSerializer(Hierarchy.class, new HierarchySerializer());
        module.addSerializer(Aspect.class, new AspectSerializer());

        mapper.registerModule(module);
        
        return mapper;
    }
}