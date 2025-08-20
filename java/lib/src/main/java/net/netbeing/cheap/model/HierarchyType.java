package net.netbeing.cheap.model;

public enum HierarchyType
{
    HIERARCHY_DIR("HL"), // Ordered directory (map) of named hierarchies in the same catalog
    ASPECT_DEF_DIR("AD"), // Directory of aspect defs, queryable by UUID or full name
    ENTITY_LIST("EL"), // List containing only entity IDs
    ENTITY_SET("ES"), // Set containing only entity IDs
    ENTITY_DIR("ED"), // String lookup of entity IDs
    ENTITY_TREE("ET"), // Tree with named nodes; leaves are entity IDs
    ASPECT_MAP("AM"); // Possibly-ordered map of entity IDs to Aspects of a single type

    private final String typeCode;

    HierarchyType(String typeCode)
    {
        this.typeCode = typeCode;
    }

    public String typeCode()
    {
        return typeCode;
    }
}
