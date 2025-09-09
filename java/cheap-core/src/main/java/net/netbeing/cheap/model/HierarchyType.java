package net.netbeing.cheap.model;

/**
 * The enum Hierarchy type.
 */
public enum HierarchyType
{
    /**
     * Hierarchy dir hierarchy type.
     */
    HIERARCHY_DIR("HL"), // Ordered directory (map) of named hierarchies in the same catalog
    /**
     * Aspect def dir hierarchy type.
     */
    ASPECT_DEF_DIR("AD"), // Directory of aspect defs, queryable by UUID or full name
    /**
     * Entity list hierarchy type.
     */
    ENTITY_LIST("EL"), // List containing only entity IDs
    /**
     * Entity set hierarchy type.
     */
    ENTITY_SET("ES"), // Set containing only entity IDs
    /**
     * Entity dir hierarchy type.
     */
    ENTITY_DIR("ED"), // String lookup of entity IDs
    /**
     * Entity tree hierarchy type.
     */
    ENTITY_TREE("ET"), // Tree with named nodes; leaves are entity IDs
    /**
     * Aspect map hierarchy type.
     */
    ASPECT_MAP("AM"); // Possibly-ordered map of entity IDs to Aspects of a single type

    private final String typeCode;

    HierarchyType(String typeCode)
    {
        this.typeCode = typeCode;
    }

    /**
     * Type code string.
     *
     * @return the string
     */
    public String typeCode()
    {
        return typeCode;
    }
}
