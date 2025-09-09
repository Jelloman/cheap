package net.netbeing.cheap.model;

/**
 * Defines the different types of hierarchies supported in the CHEAP data model.
 * Each hierarchy type serves a specific organizational purpose and has unique
 * characteristics for storing and accessing entities.
 * 
 * <p>The hierarchy types follow the CHEAP model's flexible approach to data organization,
 * allowing entities to be structured in various ways depending on the use case.</p>
 */
public enum HierarchyType
{
    /**
     * An ordered directory (map) of named hierarchies within the same catalog.
     * This is used to organize and provide access to other hierarchies by name.
     */
    HIERARCHY_DIR("HL"),
    
    /**
     * A directory of aspect definitions, queryable by UUID or full name.
     * This hierarchy type manages the metadata about available aspect types.
     */
    ASPECT_DEF_DIR("AD"),
    
    /**
     * An ordered list containing only entity IDs. This hierarchy type maintains
     * sequence and allows duplicate references to the same entity.
     */
    ENTITY_LIST("EL"),
    
    /**
     * An unordered set containing only unique entity IDs. This hierarchy type
     * ensures no duplicate entity references and provides efficient membership testing.
     */
    ENTITY_SET("ES"),
    
    /**
     * A string-to-entity ID mapping, providing named access to entities.
     * This hierarchy type enables dictionary-like lookups of entities by string keys.
     */
    ENTITY_DIR("ED"),
    
    /**
     * A tree structure with named nodes where leaves contain entity IDs.
     * This hierarchy type supports hierarchical organization with path-based navigation.
     */
    ENTITY_TREE("ET"),
    
    /**
     * A possibly-ordered map of entity IDs to aspects of a single type.
     * This hierarchy type provides efficient access to all entities having a specific aspect.
     */
    ASPECT_MAP("AM");

    private final String typeCode;

    HierarchyType(String typeCode)
    {
        this.typeCode = typeCode;
    }

    /**
     * Returns the short string code that identifies this hierarchy type.
     * These codes are used for serialization and compact representation.
     *
     * @return the two-character type code for this hierarchy type, never null
     */
    public String typeCode()
    {
        return typeCode;
    }
}
