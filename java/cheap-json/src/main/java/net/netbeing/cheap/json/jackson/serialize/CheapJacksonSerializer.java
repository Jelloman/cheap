package net.netbeing.cheap.json.jackson.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.netbeing.cheap.model.*;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Jackson-based JSON serializer for CHEAP data model objects.
 * Provides the same functionality as CheapJsonRawSerializer but uses Jackson
 * for more flexible and extensible JSON generation.
 * 
 * <p>This class provides methods for serializing CHEAP model objects to JSON format,
 * as defined by the JSON schemas in this module.</p>
 * 
 * <p>Uses custom Jackson serializers for each CHEAP element type since
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
            throw new RuntimeException("Failed to serialize catalog to JSON", e);
        }
    }
    
    /**
     * Creates and configures an ObjectMapper with custom serializers for CHEAP types.
     */
    private static ObjectMapper createMapper(boolean prettyPrint)
    {
        ObjectMapper mapper = new ObjectMapper();
        
        if (prettyPrint) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        
        SimpleModule module = new SimpleModule("CheapModule");
        
        // Register custom serializers for each CHEAP type
        module.addSerializer(Catalog.class, new CatalogSerializer());
        module.addSerializer(CatalogDef.class, new CatalogDefSerializer());
        module.addSerializer(AspectDef.class, new AspectDefSerializer());
        module.addSerializer(PropertyDef.class, new PropertyDefSerializer());
        module.addSerializer(HierarchyDef.class, new HierarchyDefSerializer());
        module.addSerializer(Hierarchy.class, new HierarchySerializer());
        module.addSerializer(Aspect.class, new AspectSerializer());
        module.addSerializer(Property.class, new PropertySerializer());
        
        // Register serializers for hierarchy subtypes (both interfaces and implementations)
        module.addSerializer(AspectMapHierarchy.class, new AspectMapHierarchySerializer());
        module.addSerializer(EntityDirectoryHierarchy.class, new EntityDirectoryHierarchySerializer());
        module.addSerializer(EntityListHierarchy.class, new EntityListHierarchySerializer());
        module.addSerializer(EntitySetHierarchy.class, new EntitySetHierarchySerializer());
        module.addSerializer(EntityTreeHierarchy.class, new EntityTreeHierarchySerializer());
        module.addSerializer(EntityTreeHierarchy.Node.class, new TreeNodeSerializer());
        
        // Also register for implementation classes that might be used
        try {
            // Use Class.forName to handle implementation classes that might not be in the module path
            @SuppressWarnings("unchecked")
            Class<AspectMapHierarchy> aspectMapHierarchyImpl = (Class<AspectMapHierarchy>) Class.forName("net.netbeing.cheap.impl.basic.AspectMapHierarchyImpl");
            @SuppressWarnings("unchecked")
            Class<EntityDirectoryHierarchy> entityDirectoryHierarchyImpl = (Class<EntityDirectoryHierarchy>) Class.forName("net.netbeing.cheap.impl.basic.EntityDirectoryHierarchyImpl");
            @SuppressWarnings("unchecked")
            Class<EntityListHierarchy> entityListHierarchyImpl = (Class<EntityListHierarchy>) Class.forName("net.netbeing.cheap.impl.basic.EntityListHierarchyImpl");
            @SuppressWarnings("unchecked")
            Class<EntitySetHierarchy> entitySetHierarchyImpl = (Class<EntitySetHierarchy>) Class.forName("net.netbeing.cheap.impl.basic.EntitySetHierarchyImpl");
            @SuppressWarnings("unchecked")
            Class<EntityTreeHierarchy> entityTreeHierarchyImpl = (Class<EntityTreeHierarchy>) Class.forName("net.netbeing.cheap.impl.basic.EntityTreeHierarchyImpl");

            module.addSerializer(aspectMapHierarchyImpl, new AspectMapHierarchySerializer());
            module.addSerializer(entityDirectoryHierarchyImpl, new EntityDirectoryHierarchySerializer());
            module.addSerializer(entityListHierarchyImpl, new EntityListHierarchySerializer());
            module.addSerializer(entitySetHierarchyImpl, new EntitySetHierarchySerializer());
            module.addSerializer(entityTreeHierarchyImpl, new EntityTreeHierarchySerializer());
        } catch (ClassNotFoundException e) {
            // Implementation classes not available, will fall back to interface serializers
        }
        
        mapper.registerModule(module);
        
        return mapper;
    }
}