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

package net.netbeing.cheap.model;

/**
 * Defines the different types of hierarchies supported in the Cheap data model.
 * Each hierarchy type serves a specific organizational purpose and has unique
 * characteristics for storing and accessing entities.
 * 
 * <p>The hierarchy types follow the Cheap model's flexible approach to data organization,
 * allowing entities to be structured in various ways depending on the use case.</p>
 */
public enum HierarchyType
{
    /**
     * An ordered list containing only entity IDs. This hierarchy type maintains
     * sequence and allows duplicate references to the same entity.
     */
    ENTITY_LIST("EL"),
    
    /**
     * A possibly-ordered set containing only unique entity IDs. This hierarchy type
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

    /**
     * Convert a type code string to a TypeCode.
     *
     * @param typeCode a 2-letter string code
     * @return the corresponding TypeCode
     * @throws IllegalArgumentException if the code is not recognized
     */
    public static HierarchyType fromTypeCode(String typeCode)
    {
        return switch (typeCode.toUpperCase()) {
            case "EL" -> HierarchyType.ENTITY_LIST;
            case "ES" -> HierarchyType.ENTITY_SET;
            case "ED" -> HierarchyType.ENTITY_DIR;
            case "ET" -> HierarchyType.ENTITY_TREE;
            case "AM" -> HierarchyType.ASPECT_MAP;
            default -> throw new IllegalArgumentException("Unknown hierarchy type code: " + typeCode);
        };
    }

}
