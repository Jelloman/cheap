package net.netbeing.cheap.json;

import net.netbeing.cheap.model.*;

import java.util.Collection;
import java.util.Map;

/**
 * Utility class for working with JSON representations of CHEAP data model objects.
 * 
 * <p>This class provides methods for serializing and deserializing CHEAP model objects
 * to and from JSON format, as well as validation against the JSON schemas defined
 * in this module.</p>
 * 
 * <p>The JSON schemas are located in the resources/json directory and define the
 * structure for all CHEAP model components including Catalogs, Hierarchies, Entities,
 * Aspects, and Properties.</p>
 */
public class CheapJsonUtil
{
    private CheapJsonUtil()
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
        StringBuilder sb = new StringBuilder();
        catalogToJson(catalog, sb);
        return sb.toString();
    }
    
    // ========== Main Schema Types ==========
    
    /**
     * Converts a Catalog to JSON.
     */
    public static void catalogToJson(Catalog catalog, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"globalId\":\"").append(escapeJson(catalog.globalId().toString())).append("\",");
        sb.append("\"uri\":\"").append(escapeJson(catalog.uri().toString())).append("\",");
        sb.append("\"species\":\"").append(catalog.species().name().toLowerCase()).append("\",");
        sb.append("\"strict\":").append(catalog.isStrict()).append(",");
        
        sb.append("\"def\":");
        catalogDefToJson(catalog.def(), sb);
        sb.append(",");
        
        if (catalog.upstream() != null) {
            sb.append("\"upstream\":\"").append(escapeJson(catalog.upstream().globalId().toString())).append("\",");
        }
        
        sb.append("\"hierarchies\":{");
        boolean first = true;
        for (Map.Entry<String, Hierarchy> entry : catalog.hierarchies().entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            hierarchyToJson(entry.getValue(), sb);
            first = false;
        }
        sb.append("},");
        
        sb.append("\"aspectDefs\":{");
        first = true;
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(aspectDef.name())).append("\":");
            aspectDefToJson(aspectDef, sb);
            first = false;
        }
        sb.append("},");
        
        sb.append("\"aspects\":{");
        first = true;
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            AspectMapHierarchy aspectMap = catalog.aspects(aspectDef);
            if (aspectMap != null && !aspectMap.isEmpty()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(aspectDef.name())).append("\":{");
                boolean firstAspect = true;
                for (Map.Entry<Entity, Aspect> aspectEntry : aspectMap.entrySet()) {
                    if (!firstAspect) sb.append(",");
                    sb.append("\"").append(escapeJson(aspectEntry.getKey().globalId().toString())).append("\":");
                    aspectToJson(aspectEntry.getValue(), sb);
                    firstAspect = false;
                }
                sb.append("}");
                first = false;
            }
        }
        sb.append("}");
        
        sb.append("}");
    }
    
    /**
     * Converts a CatalogDef to JSON.
     */
    public static void catalogDefToJson(CatalogDef catalogDef, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"aspectDefs\":{");
        boolean first = true;
        for (AspectDef aspectDef : catalogDef.aspectDefs()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(aspectDef.name())).append("\":");
            aspectDefToJson(aspectDef, sb);
            first = false;
        }
        sb.append("},");
        
        sb.append("\"hierarchyDefs\":[");
        first = true;
        for (HierarchyDef hierarchyDef : catalogDef.hierarchyDefs()) {
            if (!first) sb.append(",");
            hierarchyDefToJson(hierarchyDef, sb);
            first = false;
        }
        sb.append("]");
        sb.append("}");
    }
    
    /**
     * Converts an AspectDef to JSON.
     */
    public static void aspectDefToJson(AspectDef aspectDef, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(aspectDef.name())).append("\",");
        
        sb.append("\"propertyDefs\":[");
        boolean first = true;
        for (PropertyDef propertyDef : aspectDef.propertyDefs()) {
            if (!first) sb.append(",");
            propertyDefToJson(propertyDef, sb);
            first = false;
        }
        sb.append("],");
        
        sb.append("\"isReadable\":").append(aspectDef.isReadable()).append(",");
        sb.append("\"isWritable\":").append(aspectDef.isWritable()).append(",");
        sb.append("\"canAddProperties\":").append(aspectDef.canAddProperties()).append(",");
        sb.append("\"canRemoveProperties\":").append(aspectDef.canRemoveProperties());
        sb.append("}");
    }
    
    /**
     * Converts a PropertyDef to JSON.
     */
    public static void propertyDefToJson(PropertyDef propertyDef, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(propertyDef.name())).append("\",");
        sb.append("\"type\":\"").append(propertyDef.type().name()).append("\",");
        
        if (propertyDef.hasDefaultValue()) {
            sb.append("\"hasDefaultValue\":true,");
            sb.append("\"defaultValue\":");
            valueToJson(propertyDef.defaultValue(), sb);
            sb.append(",");
        } else {
            sb.append("\"hasDefaultValue\":false,");
        }
        
        sb.append("\"isReadable\":").append(propertyDef.isReadable()).append(",");
        sb.append("\"isWritable\":").append(propertyDef.isWritable()).append(",");
        sb.append("\"isNullable\":").append(propertyDef.isNullable()).append(",");
        sb.append("\"isRemovable\":").append(propertyDef.isRemovable()).append(",");
        sb.append("\"isMultivalued\":").append(propertyDef.isMultivalued());
        sb.append("}");
    }
    
    /**
     * Converts a HierarchyDef to JSON.
     */
    public static void hierarchyDefToJson(HierarchyDef hierarchyDef, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(hierarchyDef.name())).append("\",");
        sb.append("\"type\":\"").append(hierarchyDef.type().typeCode().toLowerCase()).append("\",");
        sb.append("\"isModifiable\":").append(hierarchyDef.isModifiable());
        sb.append("}");
    }
    
    /**
     * Converts a Hierarchy to JSON.
     */
    public static void hierarchyToJson(Hierarchy hierarchy, StringBuilder sb)
    {
        HierarchyType type = hierarchy.def().type();
        
        switch (type) {
            case ASPECT_DEF_DIR -> aspectDefDirHierarchyToJson((AspectDefDirHierarchy) hierarchy, sb);
            case ASPECT_MAP -> aspectMapHierarchyToJson((AspectMapHierarchy) hierarchy, sb);
            case ENTITY_DIR -> entityDirectoryHierarchyToJson((EntityDirectoryHierarchy) hierarchy, sb);
            case ENTITY_LIST -> entityListHierarchyToJson((EntityListHierarchy) hierarchy, sb);
            case ENTITY_SET -> entitySetHierarchyToJson((EntitySetHierarchy) hierarchy, sb);
            case ENTITY_TREE -> entityTreeHierarchyToJson((EntityTreeHierarchy) hierarchy, sb);
            case HIERARCHY_DIR -> hierarchyDirToJson((HierarchyDir) hierarchy, sb);
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + type);
        }
    }
    
    /**
     * Converts an Aspect to JSON.
     */
    public static void aspectToJson(Aspect aspect, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"aspectDefName\":\"").append(escapeJson(aspect.def().name())).append("\",");
        sb.append("\"entityId\":\"").append(escapeJson(aspect.entity().globalId().toString())).append("\",");
        sb.append("\"isTransferable\":").append(aspect.isTransferable());
        
        // Add all properties
        for (PropertyDef propertyDef : aspect.def().propertyDefs()) {
            Object value = aspect.unsafeReadObj(propertyDef.name());
            if (value != null) {
                sb.append(",\"").append(escapeJson(propertyDef.name())).append("\":");
                valueToJson(value, sb);
            }
        }
        
        sb.append("}");
    }
    
    /**
     * Converts a Property to JSON.
     */
    public static void propertyToJson(Property property, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"def\":");
        propertyDefToJson(property.def(), sb);
        sb.append(",\"value\":");
        valueToJson(property.unsafeRead(), sb);
        sb.append("}");
    }
    
    // ========== Hierarchy Sub-types ==========
    
    /**
     * Converts an AspectDefDirHierarchy to JSON.
     */
    public static void aspectDefDirHierarchyToJson(AspectDefDirHierarchy hierarchy, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"def\":{\"type\":\"aspect_def_dir\"},");
        sb.append("\"aspectDefs\":{");
        boolean first = true;
        for (AspectDef aspectDef : hierarchy.aspectDefs()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(aspectDef.name())).append("\":");
            aspectDefToJson(aspectDef, sb);
            first = false;
        }
        sb.append("}");
        sb.append("}");
    }
    
    /**
     * Converts an AspectMapHierarchy to JSON.
     */
    public static void aspectMapHierarchyToJson(AspectMapHierarchy hierarchy, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"def\":{\"type\":\"aspect_map\"},");
        sb.append("\"aspectDefName\":\"").append(escapeJson(hierarchy.aspectDef().name())).append("\",");
        sb.append("\"aspects\":{");
        boolean first = true;
        for (Map.Entry<Entity, Aspect> entry : hierarchy.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey().globalId().toString())).append("\":");
            aspectToJson(entry.getValue(), sb);
            first = false;
        }
        sb.append("}");
        sb.append("}");
    }
    
    /**
     * Converts an EntityDirectoryHierarchy to JSON.
     */
    public static void entityDirectoryHierarchyToJson(EntityDirectoryHierarchy hierarchy, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"def\":{\"type\":\"entity_dir\"},");
        sb.append("\"entities\":{");
        boolean first = true;
        for (Map.Entry<String, Entity> entry : hierarchy.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"");
            sb.append(escapeJson(entry.getValue().globalId().toString())).append("\"");
            first = false;
        }
        sb.append("}");
        sb.append("}");
    }
    
    /**
     * Converts an EntityListHierarchy to JSON.
     */
    public static void entityListHierarchyToJson(EntityListHierarchy hierarchy, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"def\":{\"type\":\"entity_list\"},");
        sb.append("\"entities\":[");
        boolean first = true;
        for (Entity entity : hierarchy) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entity.globalId().toString())).append("\"");
            first = false;
        }
        sb.append("]");
        sb.append("}");
    }
    
    /**
     * Converts an EntitySetHierarchy to JSON.
     */
    public static void entitySetHierarchyToJson(EntitySetHierarchy hierarchy, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"def\":{\"type\":\"entity_set\"},");
        sb.append("\"entities\":[");
        boolean first = true;
        for (Entity entity : hierarchy) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entity.globalId().toString())).append("\"");
            first = false;
        }
        sb.append("]");
        sb.append("}");
    }
    
    /**
     * Converts an EntityTreeHierarchy to JSON.
     */
    public static void entityTreeHierarchyToJson(EntityTreeHierarchy hierarchy, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"def\":{\"type\":\"entity_tree\"},");
        sb.append("\"root\":");
        treeNodeToJson(hierarchy.root(), sb);
        sb.append("}");
    }
    
    /**
     * Converts a HierarchyDir to JSON.
     */
    public static void hierarchyDirToJson(HierarchyDir hierarchy, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"def\":{\"type\":\"hierarchy_dir\"},");
        sb.append("\"hierarchies\":{");
        boolean first = true;
        for (Map.Entry<String, Hierarchy> entry : hierarchy.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"");
            sb.append(escapeJson(entry.getValue().def().name())).append("\"");
            first = false;
        }
        sb.append("}");
        sb.append("}");
    }
    
    /**
     * Converts a TreeNode to JSON.
     */
    public static void treeNodeToJson(EntityTreeHierarchy.Node node, StringBuilder sb)
    {
        sb.append("{");
        
        if (node.value() != null) {
            sb.append("\"entityId\":\"").append(escapeJson(node.value().globalId().toString())).append("\"");
        }
        
        if (!node.isEmpty()) {
            if (node.value() != null) sb.append(",");
            sb.append("\"children\":{");
            boolean first = true;
            for (Map.Entry<String, EntityTreeHierarchy.Node> entry : node.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                treeNodeToJson(entry.getValue(), sb);
                first = false;
            }
            sb.append("}");
        }
        
        sb.append("}");
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Converts a value to JSON representation.
     */
    private static void valueToJson(Object value, StringBuilder sb)
    {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Collection<?> collection) {
            sb.append("[");
            boolean first = true;
            for (Object item : collection) {
                if (!first) sb.append(",");
                valueToJson(item, sb);
                first = false;
            }
            sb.append("]");
        } else {
            // Fallback for other types - convert to string
            sb.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }
    
    /**
     * Escapes a string for JSON.
     */
    private static String escapeJson(String str)
    {
        if (str == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append("\\u").append(String.format("%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}