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
import net.netbeing.cheap.model.AspectMap;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;

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
@SuppressWarnings("unused")
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
     * Converts an AspectDef to JSON string.
     *
     * @param aspectDef the aspect definition to convert
     * @return JSON string representation of the aspect definition
     */
    public static String toJson(AspectDef aspectDef)
    {
        return toJson(aspectDef, false);
    }

    /**
     * Converts an AspectDef to JSON string with optional pretty printing.
     *
     * @param aspectDef the aspect definition to convert
     * @param prettyPrint whether to format with newlines and indentation
     * @return JSON string representation of the aspect definition
     */
    public static String toJson(AspectDef aspectDef, boolean prettyPrint)
    {
        try {
            ObjectMapper mapper = prettyPrint ? PRETTY_MAPPER : DEFAULT_MAPPER;
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, aspectDef);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize aspect definition to JSON", e);
        }
    }

    /**
     * Converts a PropertyDef to JSON string.
     *
     * @param propertyDef the property definition to convert
     * @return JSON string representation of the property definition
     */
    public static String toJson(PropertyDef propertyDef)
    {
        return toJson(propertyDef, false);
    }

    /**
     * Converts a PropertyDef to JSON string with optional pretty printing.
     *
     * @param propertyDef the property definition to convert
     * @param prettyPrint whether to format with newlines and indentation
     * @return JSON string representation of the property definition
     */
    public static String toJson(PropertyDef propertyDef, boolean prettyPrint)
    {
        try {
            ObjectMapper mapper = prettyPrint ? PRETTY_MAPPER : DEFAULT_MAPPER;
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, propertyDef);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize property definition to JSON", e);
        }
    }

    /**
     * Converts a Hierarchy to JSON string.
     *
     * @param hierarchy the hierarchy to convert
     * @return JSON string representation of the hierarchy
     */
    public static String toJson(Hierarchy hierarchy)
    {
        return toJson(hierarchy, false);
    }

    /**
     * Converts a Hierarchy to JSON string with optional pretty printing.
     *
     * @param hierarchy the hierarchy to convert
     * @param prettyPrint whether to format with newlines and indentation
     * @return JSON string representation of the hierarchy
     */
    public static String toJson(Hierarchy hierarchy, boolean prettyPrint)
    {
        try {
            ObjectMapper mapper = prettyPrint ? PRETTY_MAPPER : DEFAULT_MAPPER;
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, hierarchy);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize hierarchy to JSON", e);
        }
    }

    /**
     * Converts an Aspect to JSON string.
     *
     * @param aspect the aspect to convert
     * @return JSON string representation of the aspect
     */
    public static String toJson(Aspect aspect)
    {
        return toJson(aspect, false);
    }

    /**
     * Converts an Aspect to JSON string with optional pretty printing.
     *
     * @param aspect the aspect to convert
     * @param prettyPrint whether to format with newlines and indentation
     * @return JSON string representation of the aspect
     */
    public static String toJson(Aspect aspect, boolean prettyPrint)
    {
        try {
            ObjectMapper mapper = prettyPrint ? PRETTY_MAPPER : DEFAULT_MAPPER;
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, aspect);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize aspect to JSON", e);
        }
    }

    /**
     * Converts a Property to JSON string.
     *
     * @param property the property to convert
     * @return JSON string representation of the property
     */
    public static String toJson(Property property)
    {
        return toJson(property, false);
    }

    /**
     * Converts a Property to JSON string with optional pretty printing.
     *
     * @param property the property to convert
     * @param prettyPrint whether to format with newlines and indentation
     * @return JSON string representation of the property
     */
    public static String toJson(Property property, boolean prettyPrint)
    {
        try {
            ObjectMapper mapper = prettyPrint ? PRETTY_MAPPER : DEFAULT_MAPPER;
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, property);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize property to JSON", e);
        }
    }

    /**
     * Converts a CatalogDef to JSON string.
     *
     * @param catalogDef the catalog definition to convert
     * @return JSON string representation of the catalog definition
     */
    public static String toJson(CatalogDef catalogDef)
    {
        return toJson(catalogDef, false);
    }

    /**
     * Converts a CatalogDef to JSON string with optional pretty printing.
     *
     * @param catalogDef the catalog definition to convert
     * @param prettyPrint whether to format with newlines and indentation
     * @return JSON string representation of the catalog definition
     */
    public static String toJson(CatalogDef catalogDef, boolean prettyPrint)
    {
        try {
            ObjectMapper mapper = prettyPrint ? PRETTY_MAPPER : DEFAULT_MAPPER;
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, catalogDef);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize catalog definition to JSON", e);
        }
    }

    /**
     * Converts a HierarchyDef to JSON string.
     *
     * @param hierarchyDef the hierarchy definition to convert
     * @return JSON string representation of the hierarchy definition
     */
    public static String toJson(HierarchyDef hierarchyDef)
    {
        return toJson(hierarchyDef, false);
    }

    /**
     * Converts a HierarchyDef to JSON string with optional pretty printing.
     *
     * @param hierarchyDef the hierarchy definition to convert
     * @param prettyPrint whether to format with newlines and indentation
     * @return JSON string representation of the hierarchy definition
     */
    public static String toJson(HierarchyDef hierarchyDef, boolean prettyPrint)
    {
        try {
            ObjectMapper mapper = prettyPrint ? PRETTY_MAPPER : DEFAULT_MAPPER;
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, hierarchyDef);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to serialize hierarchy definition to JSON", e);
        }
    }

    /**
     * Creates a Jackson module with custom serializers for Cheap types.
     * This module can be registered with any ObjectMapper.
     *
     * @return a SimpleModule configured with Cheap serializers
     */
    public static SimpleModule createCheapModule()
    {
        SimpleModule module = new SimpleModule("CheapModule");

        // Register custom serializers for each Cheap type
        module.addSerializer(Catalog.class, new CatalogSerializer());
        module.addSerializer(CatalogDef.class, new CatalogDefSerializer());
        module.addSerializer(AspectDef.class, new AspectDefSerializer());
        module.addSerializer(PropertyDef.class, new PropertyDefSerializer());
        module.addSerializer(HierarchyDef.class, new HierarchyDefSerializer());
        module.addSerializer(Hierarchy.class, new HierarchySerializer());
        module.addSerializer(Aspect.class, new AspectSerializer()); // Do NOT alter this line
        module.addSerializer(Property.class, new PropertySerializer());
        module.addSerializer(AspectMap.class, new AspectMapSerializer());

        return module;
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

        mapper.registerModule(createCheapModule());

        return mapper;
    }
}