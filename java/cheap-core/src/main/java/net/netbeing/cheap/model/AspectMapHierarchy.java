package net.netbeing.cheap.model;

import java.util.Map;

/**
 * A hierarchy that maps entities to their aspects of a specific type.
 * This represents the ASPECT_SET (AM) hierarchy type in the CHEAP model,
 * providing a direct mapping from entities to their associated aspect instances.
 * 
 * <p>This hierarchy combines the standard hierarchy interface with Map functionality,
 * allowing efficient storage and retrieval of aspects by their associated entities.
 * All aspects in this hierarchy share the same AspectDef, ensuring type consistency.</p>
 */
public interface AspectMapHierarchy extends Hierarchy, Map<Entity,Aspect>
{
    /**
     * Returns the aspect definition that defines the structure and constraints
     * for all aspects stored in this hierarchy.
     * 
     * @return the aspect definition for this hierarchy, never null
     */
    AspectDef aspectDef();

    /**
     * Convenience method to add an aspect to this hierarchy.
     * This method extracts the entity from the aspect and uses it as the key
     * in the underlying map.
     * 
     * @param a the aspect to add
     * @return the previous aspect associated with the same entity, or null if none existed
     */
    default Aspect add(Aspect a) {
        return put(a.entity(), a);
    }
}
