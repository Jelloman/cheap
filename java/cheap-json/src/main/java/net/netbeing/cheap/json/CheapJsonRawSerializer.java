package net.netbeing.cheap.json;

import net.netbeing.cheap.model.*;

import java.util.Collection;
import java.util.Map;

/**
 * Utility class for generating JSON representations of CHEAP data model objects
 * with no external dependencies. Faster but less flexible than Jackson/GSON versions.
 * 
 * <p>This class provides methods for serializing CHEAP model objects to JSON format,
 * as defined by the JSON schemas in this module.</p>
 * 
 * <p>The JSON schemas are located in the resources/json directory and define the
 * structure for all CHEAP model components including Catalogs, Hierarchies, Entities,
 * Aspects, and Properties.</p>
 */
public class CheapJsonRawSerializer
{
    private CheapJsonRawSerializer()
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
        StringBuilder sb = new StringBuilder();
        catalogToJson(catalog, sb, prettyPrint, 0);
        return sb.toString();
    }
    
    // ========== Main Schema Types ==========
    
    /**
     * Converts a Catalog to JSON.
     */
    public static void catalogToJson(Catalog catalog, StringBuilder sb)
    {
        catalogToJson(catalog, sb, false, 0);
    }
    
    /**
     * Converts a Catalog to JSON and returns as a String.
     */
    public static String catalogToJson(Catalog catalog)
    {
        StringBuilder sb = new StringBuilder();
        catalogToJson(catalog, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts a Catalog to JSON with optional pretty printing.
     */
    public static void catalogToJson(Catalog catalog, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"globalId\":\"").append(escapeJson(catalog.globalId().toString())).append("\",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        if (catalog.uri() != null) {
            sb.append("\"uri\":\"").append(escapeJson(catalog.uri().toString())).append("\",");
        } else {
            sb.append("\"uri\":null,");
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"species\":\"").append(catalog.species().name().toLowerCase()).append("\",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"strict\":").append(catalog.isStrict()).append(",");
        
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"def\":");
        catalogDefToJson(catalog.def(), sb, prettyPrint, indent + 1);
        sb.append(",");
        
        if (catalog.upstream() != null) {
            appendNewlineAndIndent(sb, prettyPrint, indent + 1);
            sb.append("\"upstream\":\"").append(escapeJson(catalog.upstream().globalId().toString())).append("\",");
        }
        
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"hierarchies\":{");
        boolean first = true;
        for (Map.Entry<String, Hierarchy> entry : catalog.hierarchies().entrySet()) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            hierarchyToJson(entry.getValue(), sb, prettyPrint, indent + 2);
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("},");
        
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"aspectDefs\":{");
        first = true;
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            sb.append("\"").append(escapeJson(aspectDef.name())).append("\":");
            aspectDefToJson(aspectDef, sb, prettyPrint, indent + 2);
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("},");
        
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"aspects\":{");
        first = true;
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            AspectMapHierarchy aspectMap = catalog.aspects(aspectDef);
            if (aspectMap != null && !aspectMap.isEmpty()) {
                if (!first) sb.append(",");
                appendNewlineAndIndent(sb, prettyPrint, indent + 2);
                sb.append("\"").append(escapeJson(aspectDef.name())).append("\":{");
                boolean firstAspect = true;
                for (Map.Entry<Entity, Aspect> aspectEntry : aspectMap.entrySet()) {
                    if (!firstAspect) sb.append(",");
                    appendNewlineAndIndent(sb, prettyPrint, indent + 3);
                    sb.append("\"").append(escapeJson(aspectEntry.getKey().globalId().toString())).append("\":");
                    aspectToJson(aspectEntry.getValue(), sb, prettyPrint, indent + 3);
                    firstAspect = false;
                }
                appendNewlineAndIndent(sb, prettyPrint, indent + 2);
                sb.append("}");
                first = false;
            }
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("}");
        
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts a CatalogDef to JSON.
     */
    public static void catalogDefToJson(CatalogDef catalogDef, StringBuilder sb)
    {
        catalogDefToJson(catalogDef, sb, false, 0);
    }
    
    /**
     * Converts a CatalogDef to JSON and returns as a String.
     */
    public static String catalogDefToJson(CatalogDef catalogDef)
    {
        StringBuilder sb = new StringBuilder();
        catalogDefToJson(catalogDef, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts a CatalogDef to JSON with optional pretty printing.
     */
    public static void catalogDefToJson(CatalogDef catalogDef, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"aspectDefs\":{");
        boolean first = true;
        for (AspectDef aspectDef : catalogDef.aspectDefs()) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            sb.append("\"").append(escapeJson(aspectDef.name())).append("\":");
            aspectDefToJson(aspectDef, sb, prettyPrint, indent + 2);
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("},");
        
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"hierarchyDefs\":[");
        first = true;
        for (HierarchyDef hierarchyDef : catalogDef.hierarchyDefs()) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            hierarchyDefToJson(hierarchyDef, sb, prettyPrint, indent + 2);
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("]");
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts an AspectDef to JSON.
     */
    public static void aspectDefToJson(AspectDef aspectDef, StringBuilder sb)
    {
        aspectDefToJson(aspectDef, sb, false, 0);
    }
    
    /**
     * Converts an AspectDef to JSON and returns as a String.
     */
    public static String aspectDefToJson(AspectDef aspectDef)
    {
        StringBuilder sb = new StringBuilder();
        aspectDefToJson(aspectDef, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts an AspectDef to JSON with optional pretty printing.
     */
    public static void aspectDefToJson(AspectDef aspectDef, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"name\":\"").append(escapeJson(aspectDef.name())).append("\",");
        
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"propertyDefs\":[");
        boolean first = true;
        for (PropertyDef propertyDef : aspectDef.propertyDefs()) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            propertyDefToJson(propertyDef, sb, prettyPrint, indent + 2);
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("],");
        
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"isReadable\":").append(aspectDef.isReadable()).append(",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"isWritable\":").append(aspectDef.isWritable()).append(",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"canAddProperties\":").append(aspectDef.canAddProperties()).append(",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"canRemoveProperties\":").append(aspectDef.canRemoveProperties());
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts a PropertyDef to JSON.
     */
    public static void propertyDefToJson(PropertyDef propertyDef, StringBuilder sb)
    {
        propertyDefToJson(propertyDef, sb, false, 0);
    }
    
    /**
     * Converts a PropertyDef to JSON and returns as a String.
     */
    public static String propertyDefToJson(PropertyDef propertyDef)
    {
        StringBuilder sb = new StringBuilder();
        propertyDefToJson(propertyDef, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts a PropertyDef to JSON with optional pretty printing.
     */
    public static void propertyDefToJson(PropertyDef propertyDef, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"name\":\"").append(escapeJson(propertyDef.name())).append("\",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"type\":\"").append(propertyDef.type().name()).append("\",");
        
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        if (propertyDef.hasDefaultValue()) {
            sb.append("\"hasDefaultValue\":true,");
            appendNewlineAndIndent(sb, prettyPrint, indent + 1);
            sb.append("\"defaultValue\":");
            valueToJson(propertyDef.defaultValue(), sb, prettyPrint, indent + 1);
            sb.append(",");
        } else {
            sb.append("\"hasDefaultValue\":false,");
        }
        
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"isReadable\":").append(propertyDef.isReadable()).append(",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"isWritable\":").append(propertyDef.isWritable()).append(",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"isNullable\":").append(propertyDef.isNullable()).append(",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"isRemovable\":").append(propertyDef.isRemovable()).append(",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"isMultivalued\":").append(propertyDef.isMultivalued());
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts a HierarchyDef to JSON.
     */
    public static void hierarchyDefToJson(HierarchyDef hierarchyDef, StringBuilder sb)
    {
        hierarchyDefToJson(hierarchyDef, sb, false, 0);
    }
    
    /**
     * Converts a HierarchyDef to JSON and returns as a String.
     */
    public static String hierarchyDefToJson(HierarchyDef hierarchyDef)
    {
        StringBuilder sb = new StringBuilder();
        hierarchyDefToJson(hierarchyDef, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts a HierarchyDef to JSON with optional pretty printing.
     */
    public static void hierarchyDefToJson(HierarchyDef hierarchyDef, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"name\":\"").append(escapeJson(hierarchyDef.name())).append("\",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"type\":\"").append(hierarchyDef.type().typeCode().toLowerCase()).append("\",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"isModifiable\":").append(hierarchyDef.isModifiable());
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts a Hierarchy to JSON.
     */
    public static void hierarchyToJson(Hierarchy hierarchy, StringBuilder sb)
    {
        hierarchyToJson(hierarchy, sb, false, 0);
    }
    
    /**
     * Converts a Hierarchy to JSON and returns as a String.
     */
    public static String hierarchyToJson(Hierarchy hierarchy)
    {
        StringBuilder sb = new StringBuilder();
        hierarchyToJson(hierarchy, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts a Hierarchy to JSON with optional pretty printing.
     */
    public static void hierarchyToJson(Hierarchy hierarchy, StringBuilder sb, boolean prettyPrint, int indent)
    {
        HierarchyType type = hierarchy.def().type();
        
        switch (type) {
            case ASPECT_DEF_DIR -> aspectDefDirHierarchyToJson((AspectDefDirHierarchy) hierarchy, sb, prettyPrint, indent);
            case ASPECT_MAP -> aspectMapHierarchyToJson((AspectMapHierarchy) hierarchy, sb, prettyPrint, indent);
            case ENTITY_DIR -> entityDirectoryHierarchyToJson((EntityDirectoryHierarchy) hierarchy, sb, prettyPrint, indent);
            case ENTITY_LIST -> entityListHierarchyToJson((EntityListHierarchy) hierarchy, sb, prettyPrint, indent);
            case ENTITY_SET -> entitySetHierarchyToJson((EntitySetHierarchy) hierarchy, sb, prettyPrint, indent);
            case ENTITY_TREE -> entityTreeHierarchyToJson((EntityTreeHierarchy) hierarchy, sb, prettyPrint, indent);
            case HIERARCHY_DIR -> hierarchyDirToJson((HierarchyDir) hierarchy, sb, prettyPrint, indent);
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + type);
        }
    }
    
    /**
     * Converts an Aspect to JSON.
     */
    public static void aspectToJson(Aspect aspect, StringBuilder sb)
    {
        aspectToJson(aspect, sb, false, 0);
    }
    
    /**
     * Converts an Aspect to JSON and returns as a String.
     */
    public static String aspectToJson(Aspect aspect)
    {
        StringBuilder sb = new StringBuilder();
        aspectToJson(aspect, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts an Aspect to JSON with optional pretty printing.
     */
    public static void aspectToJson(Aspect aspect, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"aspectDefName\":\"").append(escapeJson(aspect.def().name())).append("\",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"entityId\":\"").append(escapeJson(aspect.entity().globalId().toString())).append("\",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"isTransferable\":").append(aspect.isTransferable());
        
        // Add all properties
        for (PropertyDef propertyDef : aspect.def().propertyDefs()) {
            Object value = aspect.unsafeReadObj(propertyDef.name());
            if (value != null) {
                sb.append(",");
                appendNewlineAndIndent(sb, prettyPrint, indent + 1);
                sb.append("\"").append(escapeJson(propertyDef.name())).append("\":");
                valueToJson(value, sb, prettyPrint, indent + 1);
            }
        }
        
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts a Property to JSON.
     */
    public static void propertyToJson(Property property, StringBuilder sb)
    {
        propertyToJson(property, sb, false, 0);
    }
    
    /**
     * Converts a Property to JSON and returns as a String.
     */
    public static String propertyToJson(Property property)
    {
        StringBuilder sb = new StringBuilder();
        propertyToJson(property, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts a Property to JSON with optional pretty printing.
     */
    public static void propertyToJson(Property property, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"def\":");
        propertyDefToJson(property.def(), sb, prettyPrint, indent + 1);
        sb.append(",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"value\":");
        valueToJson(property.unsafeRead(), sb, prettyPrint, indent + 1);
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    // ========== Hierarchy Sub-types ==========
    
    /**
     * Converts an AspectDefDirHierarchy to JSON.
     */
    public static void aspectDefDirHierarchyToJson(AspectDefDirHierarchy hierarchy, StringBuilder sb)
    {
        aspectDefDirHierarchyToJson(hierarchy, sb, false, 0);
    }
    
    /**
     * Converts an AspectDefDirHierarchy to JSON and returns as a String.
     */
    public static String aspectDefDirHierarchyToJson(AspectDefDirHierarchy hierarchy)
    {
        StringBuilder sb = new StringBuilder();
        aspectDefDirHierarchyToJson(hierarchy, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts an AspectDefDirHierarchy to JSON with optional pretty printing.
     */
    public static void aspectDefDirHierarchyToJson(AspectDefDirHierarchy hierarchy, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"def\":{\"type\":\"aspect_def_dir\"},");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"aspectDefs\":{");
        boolean first = true;
        for (AspectDef aspectDef : hierarchy.aspectDefs()) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            sb.append("\"").append(escapeJson(aspectDef.name())).append("\":");
            aspectDefToJson(aspectDef, sb, prettyPrint, indent + 2);
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("}");
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts an AspectMapHierarchy to JSON.
     */
    public static void aspectMapHierarchyToJson(AspectMapHierarchy hierarchy, StringBuilder sb)
    {
        aspectMapHierarchyToJson(hierarchy, sb, false, 0);
    }
    
    /**
     * Converts an AspectMapHierarchy to JSON and returns as a String.
     */
    public static String aspectMapHierarchyToJson(AspectMapHierarchy hierarchy)
    {
        StringBuilder sb = new StringBuilder();
        aspectMapHierarchyToJson(hierarchy, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts an AspectMapHierarchy to JSON with optional pretty printing.
     */
    public static void aspectMapHierarchyToJson(AspectMapHierarchy hierarchy, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"def\":{\"type\":\"aspect_map\"},");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"aspectDefName\":\"").append(escapeJson(hierarchy.aspectDef().name())).append("\",");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"aspects\":{");
        boolean first = true;
        for (Map.Entry<Entity, Aspect> entry : hierarchy.entrySet()) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            sb.append("\"").append(escapeJson(entry.getKey().globalId().toString())).append("\":");
            aspectToJson(entry.getValue(), sb, prettyPrint, indent + 2);
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("}");
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts an EntityDirectoryHierarchy to JSON.
     */
    public static void entityDirectoryHierarchyToJson(EntityDirectoryHierarchy hierarchy, StringBuilder sb)
    {
        entityDirectoryHierarchyToJson(hierarchy, sb, false, 0);
    }
    
    /**
     * Converts an EntityDirectoryHierarchy to JSON and returns as a String.
     */
    public static String entityDirectoryHierarchyToJson(EntityDirectoryHierarchy hierarchy)
    {
        StringBuilder sb = new StringBuilder();
        entityDirectoryHierarchyToJson(hierarchy, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts an EntityDirectoryHierarchy to JSON with optional pretty printing.
     */
    public static void entityDirectoryHierarchyToJson(EntityDirectoryHierarchy hierarchy, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"def\":{\"type\":\"entity_dir\"},");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"entities\":{");
        boolean first = true;
        for (Map.Entry<String, Entity> entry : hierarchy.entrySet()) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"");
            sb.append(escapeJson(entry.getValue().globalId().toString())).append("\"");
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("}");
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts an EntityListHierarchy to JSON.
     */
    public static void entityListHierarchyToJson(EntityListHierarchy hierarchy, StringBuilder sb)
    {
        entityListHierarchyToJson(hierarchy, sb, false, 0);
    }
    
    /**
     * Converts an EntityListHierarchy to JSON and returns as a String.
     */
    public static String entityListHierarchyToJson(EntityListHierarchy hierarchy)
    {
        StringBuilder sb = new StringBuilder();
        entityListHierarchyToJson(hierarchy, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts an EntityListHierarchy to JSON with optional pretty printing.
     */
    public static void entityListHierarchyToJson(EntityListHierarchy hierarchy, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"def\":{\"type\":\"entity_list\"},");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"entities\":[");
        boolean first = true;
        for (Entity entity : hierarchy) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            sb.append("\"").append(escapeJson(entity.globalId().toString())).append("\"");
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("]");
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts an EntitySetHierarchy to JSON.
     */
    public static void entitySetHierarchyToJson(EntitySetHierarchy hierarchy, StringBuilder sb)
    {
        entitySetHierarchyToJson(hierarchy, sb, false, 0);
    }
    
    /**
     * Converts an EntitySetHierarchy to JSON and returns as a String.
     */
    public static String entitySetHierarchyToJson(EntitySetHierarchy hierarchy)
    {
        StringBuilder sb = new StringBuilder();
        entitySetHierarchyToJson(hierarchy, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts an EntitySetHierarchy to JSON with optional pretty printing.
     */
    public static void entitySetHierarchyToJson(EntitySetHierarchy hierarchy, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"def\":{\"type\":\"entity_set\"},");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"entities\":[");
        boolean first = true;
        for (Entity entity : hierarchy) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            sb.append("\"").append(escapeJson(entity.globalId().toString())).append("\"");
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("]");
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts an EntityTreeHierarchy to JSON.
     */
    public static void entityTreeHierarchyToJson(EntityTreeHierarchy hierarchy, StringBuilder sb)
    {
        entityTreeHierarchyToJson(hierarchy, sb, false, 0);
    }
    
    /**
     * Converts an EntityTreeHierarchy to JSON and returns as a String.
     */
    public static String entityTreeHierarchyToJson(EntityTreeHierarchy hierarchy)
    {
        StringBuilder sb = new StringBuilder();
        entityTreeHierarchyToJson(hierarchy, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts an EntityTreeHierarchy to JSON with optional pretty printing.
     */
    public static void entityTreeHierarchyToJson(EntityTreeHierarchy hierarchy, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"def\":{\"type\":\"entity_tree\"},");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"root\":");
        treeNodeToJson(hierarchy.root(), sb, prettyPrint, indent + 1);
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts a HierarchyDir to JSON.
     */
    public static void hierarchyDirToJson(HierarchyDir hierarchy, StringBuilder sb)
    {
        hierarchyDirToJson(hierarchy, sb, false, 0);
    }
    
    /**
     * Converts a HierarchyDir to JSON and returns as a String.
     */
    public static String hierarchyDirToJson(HierarchyDir hierarchy)
    {
        StringBuilder sb = new StringBuilder();
        hierarchyDirToJson(hierarchy, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts a HierarchyDir to JSON with optional pretty printing.
     */
    public static void hierarchyDirToJson(HierarchyDir hierarchy, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"def\":{\"type\":\"hierarchy_dir\"},");
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("\"hierarchies\":{");
        boolean first = true;
        for (Map.Entry<String, Hierarchy> entry : hierarchy.entrySet()) {
            if (!first) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 2);
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"");
            sb.append(escapeJson(entry.getValue().def().name())).append("\"");
            first = false;
        }
        appendNewlineAndIndent(sb, prettyPrint, indent + 1);
        sb.append("}");
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    /**
     * Converts a TreeNode to JSON.
     */
    public static void treeNodeToJson(EntityTreeHierarchy.Node node, StringBuilder sb)
    {
        treeNodeToJson(node, sb, false, 0);
    }
    
    /**
     * Converts a TreeNode to JSON and returns as a String.
     */
    public static String treeNodeToJson(EntityTreeHierarchy.Node node)
    {
        StringBuilder sb = new StringBuilder();
        treeNodeToJson(node, sb, false, 0);
        return sb.toString();
    }
    
    /**
     * Converts a TreeNode to JSON with optional pretty printing.
     */
    public static void treeNodeToJson(EntityTreeHierarchy.Node node, StringBuilder sb, boolean prettyPrint, int indent)
    {
        sb.append("{");
        
        if (node.value() != null) {
            appendNewlineAndIndent(sb, prettyPrint, indent + 1);
            sb.append("\"entityId\":\"").append(escapeJson(node.value().globalId().toString())).append("\"");
        }
        
        if (!node.isEmpty()) {
            if (node.value() != null) sb.append(",");
            appendNewlineAndIndent(sb, prettyPrint, indent + 1);
            sb.append("\"children\":{");
            boolean first = true;
            for (Map.Entry<String, EntityTreeHierarchy.Node> entry : node.entrySet()) {
                if (!first) sb.append(",");
                appendNewlineAndIndent(sb, prettyPrint, indent + 2);
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                treeNodeToJson(entry.getValue(), sb, prettyPrint, indent + 2);
                first = false;
            }
            appendNewlineAndIndent(sb, prettyPrint, indent + 1);
            sb.append("}");
        }
        
        appendNewlineAndIndent(sb, prettyPrint, indent);
        sb.append("}");
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Converts a value to JSON representation.
     */
    private static void valueToJson(Object value, StringBuilder sb)
    {
        valueToJson(value, sb, false, 0);
    }
    
    /**
     * Converts a value to JSON representation with optional pretty printing.
     */
    private static void valueToJson(Object value, StringBuilder sb, boolean prettyPrint, int indent)
    {
        switch (value) {
            case null -> sb.append("null");
            case String s -> sb.append("\"").append(escapeJson(s)).append("\"");
            case Number number -> sb.append(value.toString());
            case Boolean b -> sb.append(value.toString());
            case Collection<?> collection -> {
                sb.append("[");
                boolean first = true;
                for (Object item : collection) {
                    if (!first) sb.append(",");
                    appendNewlineAndIndent(sb, prettyPrint, indent + 1);
                    valueToJson(item, sb, prettyPrint, indent + 1);
                    first = false;
                }
                appendNewlineAndIndent(sb, prettyPrint, indent);
                sb.append("]");
            }
            default ->
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
    
    /**
     * Appends newline and indentation if pretty printing is enabled.
     */
    private static void appendNewlineAndIndent(StringBuilder sb, boolean prettyPrint, int indent)
    {
        if (prettyPrint) {
            sb.append("\n");
            sb.append(" ".repeat(Math.max(0, indent * 2)));
        }
    }
}